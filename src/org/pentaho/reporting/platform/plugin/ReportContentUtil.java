package org.pentaho.reporting.platform.plugin;

import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.table.TableModel;

import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.platform.plugin.action.jfreereport.helper.PentahoTableModel;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.engine.classic.core.util.beans.ValueConverter;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.platform.plugin.messages.Messages;

/**
 * Todo: Document me!
 * <p/>
 * Date: 28.07.2010
 * Time: 14:02:11
 *
 * @author Thomas Morgner.
 */
public class ReportContentUtil
{

  public static Object computeParameterValue(final ParameterContext report,
                                       final ParameterDefinitionEntry parameterDefinition,
                                       final Object value)
      throws ReportProcessingException
  {
    if (value == null || "".equals(value))
    {
      // there are still buggy report definitions out there ...
      final Object defaultValue = parameterDefinition.getDefaultValue(report);
      if (defaultValue == null || "".equals(defaultValue))
      {
        return null;
      }
      return defaultValue;
    }

    final Class valueType = parameterDefinition.getValueType();
    if (isAllowMultiSelect(parameterDefinition) && Collection.class.isInstance(value))
    {
      final Collection c = (Collection) value;
      final Class componentType;
      final Class parameterValueType = valueType;
      if (parameterValueType.isArray())
      {
        componentType = parameterValueType.getComponentType();
      }
      else
      {
        componentType = parameterValueType;
      }

      final int length = c.size();
      final Object[] sourceArray = c.toArray();
      final Object array = Array.newInstance(componentType, length);
      for (int i = 0; i < length; i++)
      {
        Array.set(array, i, convert(report, parameterDefinition, componentType, sourceArray[i]));
      }
      return array;
    }
    else if (value.getClass().isArray())
    {
      final Class componentType;
      if (valueType.isArray())
      {
        componentType = valueType.getComponentType();
      }
      else
      {
        componentType = valueType;
      }

      final int length = Array.getLength(value);
      final Object array = Array.newInstance(componentType, length);
      for (int i = 0; i < length; i++)
      {
        Array.set(array, i, convert(report, parameterDefinition, componentType, Array.get(value, i)));
      }
      return array;
    }
    else if (isAllowMultiSelect(parameterDefinition))
    {
      // if the parameter allows multi selections, wrap this single input in an array
      // and re-call addParameter with it
      final Object[] array = new Object[1];
      array[0] = value;
      return computeParameterValue(report, parameterDefinition, array);
    }
    else
    {
      return convert(report, parameterDefinition, parameterDefinition.getValueType(), value);
    }
  }

  private static boolean isAllowMultiSelect(final ParameterDefinitionEntry parameter)
  {
    if (parameter instanceof ListParameter)
    {
      final ListParameter listParameter = (ListParameter) parameter;
      return listParameter.isAllowMultiSelection();
    }
    return false;
  }


  private static Object convert(final ParameterContext context,
                                final ParameterDefinitionEntry parameter,
                                final Class targetType, final Object rawValue)
      throws ReportProcessingException
  {
    if (targetType == null)
    {
      throw new NullPointerException();
    }

    if (rawValue == null)
    {
      return null;
    }
    if (targetType.isInstance(rawValue))
    {
      return rawValue;
    }

    if (targetType.isAssignableFrom(TableModel.class) && IPentahoResultSet.class.isAssignableFrom(rawValue.getClass()))
    {
      // wrap IPentahoResultSet to simulate TableModel
      return new PentahoTableModel((IPentahoResultSet) rawValue);
    }

    final String valueAsString = String.valueOf(rawValue);
    if (StringUtils.isEmpty(valueAsString))
    {
      return null;
    }

    if (targetType.equals(Timestamp.class))
    {
      try
      {
        final Date date = parseDate(parameter, context, valueAsString);
        return new Timestamp(date.getTime());
      }
      catch (ParseException pe)
      {
        // ignore, we try to parse it as real date now ..
      }
    }
    else if (targetType.equals(Time.class))
    {
      try
      {
        final Date date = parseDate(parameter, context, valueAsString);
        return new Time(date.getTime());
      }
      catch (ParseException pe)
      {
        // ignore, we try to parse it as real date now ..
      }
    }
    else if (targetType.equals(java.sql.Date.class))
    {
      try
      {
        final Date date = parseDate(parameter, context, valueAsString);
        return new java.sql.Date(date.getTime());
      }
      catch (ParseException pe)
      {
        // ignore, we try to parse it as real date now ..
      }
    }
    else if (targetType.equals(Date.class))
    {
      try
      {
        final Date date = parseDate(parameter, context, valueAsString);
        return new Date(date.getTime());
      }
      catch (ParseException pe)
      {
        // ignore, we try to parse it as real date now ..
      }
    }

    final String dataFormat = parameter.getParameterAttribute(ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.DATA_FORMAT, context);
    if (dataFormat != null)
    {
      try
      {
        if (Number.class.isAssignableFrom(targetType))
        {
          final DecimalFormat format = new DecimalFormat(dataFormat, new DecimalFormatSymbols(LocaleHelper.getLocale()));
          format.setParseBigDecimal(true);
          final Number number = format.parse(valueAsString);
          final String asText = ConverterRegistry.toAttributeValue(number);
          return ConverterRegistry.toPropertyValue(asText, targetType);
        }
        else if (Date.class.isAssignableFrom(targetType))
        {
          final SimpleDateFormat format = new SimpleDateFormat(dataFormat, new DateFormatSymbols(LocaleHelper.getLocale()));
          format.setLenient(false);
          final Date number = format.parse(valueAsString);
          final String asText = ConverterRegistry.toAttributeValue(number);
          return ConverterRegistry.toPropertyValue(asText, targetType);
        }
      }
      catch (Exception e)
      {
        // again, ignore it .
      }
    }

    final ValueConverter valueConverter = ConverterRegistry.getInstance().getValueConverter(targetType);
    if (valueConverter != null)
    {
      try
      {
        return valueConverter.toPropertyValue(valueAsString);
      }
      catch (BeanException e)
      {
        throw new ReportProcessingException(Messages.getString
            ("ReportPlugin.unableToConvertParameter", parameter.getName(), valueAsString)); //$NON-NLS-1$
      }
    }
    return rawValue;
  }

  private static Date parseDate(final ParameterDefinitionEntry parameterEntry,
                                final ParameterContext context,
                                final String value) throws ParseException
  {
    try
    {
      return parseDateStrict(parameterEntry, context, value);
    }
    catch (ParseException pe)
    {
      //
    }

    try
    {
      // parse the legacy format that we used in 3.5.0-GA.
      final Long dateAsLong = Long.parseLong(value);
      return new Date(dateAsLong);
    }
    catch (NumberFormatException nfe)
    {
      // ignored
    }

    try
    {
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
      return simpleDateFormat.parse(value);
    }
    catch (ParseException pe)
    {
      //
    }
    throw new ParseException("Unable to parse Date", 0);
  }

  private static Date parseDateStrict(final ParameterDefinitionEntry parameterEntry,
                                final ParameterContext context,
                                final String value) throws ParseException
  {
    final String timezoneSpec = parameterEntry.getParameterAttribute
        (ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TIMEZONE, context);
    if (timezoneSpec == null ||
        "server".equals(timezoneSpec))
    {
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      return simpleDateFormat.parse(value);
    }
    else if ("utc".equals(timezoneSpec))
    {
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      return simpleDateFormat.parse(value);
    }
    else if ("client".equals(timezoneSpec))
    {
      try
      {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return simpleDateFormat.parse(value);
      }
      catch (ParseException pe)
      {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return simpleDateFormat.parse(value);
      }
    }
    else
    {
      final TimeZone timeZone = TimeZone.getTimeZone(timezoneSpec);
      // this never returns null, but if the timezone is not understood, we end up with GMT/UTC.
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      simpleDateFormat.setTimeZone(timeZone);
      return simpleDateFormat.parse(value);
    }
  }
  
}
