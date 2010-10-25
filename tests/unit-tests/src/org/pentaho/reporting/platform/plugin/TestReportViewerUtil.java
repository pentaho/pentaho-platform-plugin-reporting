package org.pentaho.reporting.platform.plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.pentaho.reporting.platform.plugin.gwt.client.Parameter;
import org.pentaho.reporting.platform.plugin.gwt.client.TimeZoneOffsets;

/**
 * Todo: Document me!
 * <p/>
 * Date: 25.10.2010
 * Time: 15:46:28
 *
 * @author Thomas Morgner.
 */
public class TestReportViewerUtil
{

  public static Date parseWithTimezone(final String dateString) throws ParseException
  {
    if (dateString.length() != 28)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date with timezone: " + dateString);
    }
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'Z").parse(dateString);
  }

  public static Date parseWithoutTimezone(String dateString) throws ParseException
  {
    if (dateString.length() == 28)
    {
      dateString = dateString.substring(0, 23);
    }
    if (dateString.length() != 23)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date without timezone: " + dateString);
    }
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'").parse(dateString);
  }

  public static int getNativeTimezoneOffset()
  {
    return TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000;
  }


  /**
   * Converts a time from a arbitary timezone into the local timezone. The timestamp value remains unchanged,
   * but the string representation changes to reflect the give timezone.
   *
   * @param originalTimestamp             the timestamp as string from the server.
   * @param targetTimeZoneOffsetInMinutes the target timezone offset in minutes from GMT
   * @return the converted timestamp string.
   */
  public static String convertTimeStampToTimeZone(final String originalTimestamp,
                                                  final int targetTimeZoneOffsetInMinutes) throws ParseException
  {
    final SimpleDateFormat localDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    final Date dateLocal = parseWithoutTimezone(originalTimestamp);
    final Date dateUtc = parseWithTimezone(originalTimestamp);
    final String offsetText = TimeZoneOffsets.formatOffset(targetTimeZoneOffsetInMinutes);
    final long date = dateLocal.getTime() + (targetTimeZoneOffsetInMinutes * 60000) +
        (dateUtc.getTime() - dateLocal.getTime()) - (getNativeTimezoneOffset() * 60000);

    final Date localWithShift = new Date(date);
    final String dateAsText = localDate.format(localWithShift) + offsetText;
    return dateAsText;
  }

  /**
   * @noinspection HardCodedStringLiteral
   */
  public static String normalizeParameterValue(final Parameter parameter,
                                               String type,
                                               final String selection) throws ParseException
  {
    if (selection == null || selection.length() == 0)
    {
      return null;
    }

    if (type == null)
    {
      return selection;
    }

    if (type.startsWith("[L") && type.endsWith(";")) // NON-NLS
    {
      type = type.substring(2, type.length() - 1);
    }

    if ("java.util.Date".equals(type) ||
        "java.sql.Date".equals(type) ||
        "java.sql.Time".equals(type) ||
        "java.sql.Timestamp".equals(type))
    {
      try
      {
        // date handling speciality here ...
        final String timezone = parameter.getAttribute("timezone");
        String timezoneHint = parameter.getTimezoneHint();
        if (timezone == null || "server".equals(timezone))
        {
          if (timezoneHint == null)
          {
            timezoneHint = extractTimezoneHintFromData(selection);
          }
          if (timezoneHint == null)
          {
            return selection;
          }

          // update the parameter definition, so that the datepickerUI can work properly ...
          parameter.setTimezoneHint(timezoneHint);
          return selection;
        }

        if ("client".equals(timezone))
        {
          return selection;
        }

        // for every other mode (fixed timezone modes), translate the time into the specified timezone
        if (timezoneHint != null && timezoneHint.length() > 0)
        {
          if (selection.endsWith(timezoneHint))
          {
            return selection;
          }
        }

        // the resulting time will have the same universal time as the original one, but the string
        // will match the timeoffset specified in the timezone.
        return convertTimeStampToTimeZone(selection, TimeZoneOffsets.getInstance().getOffset(timezone));
      }
      catch (IllegalArgumentException iae)
      {
        // failed to parse the text ..
        return selection;
      }
    }

    return selection;
  }

  public static String extractTimezoneHintFromData(final String dateString)
  {
    if (dateString.length() == 28)
    {
      return dateString.substring(23, 28);
    }
    else
    {
      return null;
    }
  }

}
