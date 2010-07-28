package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.Date;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import org.pentaho.gwt.widgets.client.datepicker.PentahoDatePicker;

public class DateParameterUI extends SimplePanel
{
  //private final static DateTimeFormat FORMAT = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // NON-NLS

  private class DateParameterSelectionHandler implements ValueChangeHandler<Date>, ChangeHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;
    private String timezone;
    private DateTimeFormat format;

    public DateParameterSelectionHandler(final ParameterControllerPanel controller,
                                         final Parameter parameter)
    {
      this.controller = controller;
      this.parameterName = parameter.getName();
      this.timezone = parameter.getAttribute("timezone");
      final String timezoneHint = parameter.getTimezoneHint();
      if (timezone == null || "server".equals(timezone))
      {
        // Take the date string as it comes from the server, cut out the timezone information - the
        // server will supply its own here.
        format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      }
      else if ("cient".equals(timezone))
      {
        // we ignore the timezone the server sends - any default date given by the server will
        // be interpreted to be a client-side date.
        format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      }
      else if ("utc".equals(timezone))
      {
        // Take the date string as it comes from the server, present it as local time but when sending it off,
        // do not include timezone information.
        format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'");
      }
      else if (timezoneHint != null && timezoneHint.length() != 0)
      {
        format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'" + timezoneHint + "'");
      }
      else
      {
        format = DateTimeFormat.getFormat
            ("yyyy-MM-dd'T'HH:mm:ss.SSS'" + TimeZoneOffsets.getInstance().getOffsetAsString(timezone) + "'");
      }
    }

    public void onValueChange(final ValueChangeEvent<Date> event)
    {
      final Date newDate;
      if (ReportViewerUtil.isEmpty(datePicker.getTextBox().getText()))
      {
        newDate = null;
      }
      else
      {
        newDate = event.getValue();
      }

      // add date as long
      if (newDate == null)
      {
        controller.getParameterMap().setSelectedValue(parameterName, null); //$NON-NLS-1$
      }
      else
      {
        if ("client".equals(timezone))
        {
          final String offsetText = TimeZoneOffsets.formatOffset(getNativeTimezoneOffset(newDate.getTime()));
          controller.getParameterMap().setSelectedValue(parameterName, format.format(newDate) + offsetText); //$NON-NLS-1$
        }
        else
        {
          controller.getParameterMap().setSelectedValue(parameterName, format.format(newDate));
        } //$NON-NLS-1$
      }
      controller.fetchParameters(true);
    }

    public void onChange(final ChangeEvent changeEvent)
    {
      final String s = datePicker.getTextBox().getText();
      if (ReportViewerUtil.isEmpty(s))
      {
        datePicker.setValue(null, true);
//        controller.getParameterMap().setSelectedValue(parameterName, null); //$NON-NLS-1$
      }
      else
      {
//        datePicker.setValue(null, true);
//        controller.getParameterMap().setSelectedValue(parameterName, format.format(datePicker.getValue())); //$NON-NLS-1$
      }
//      controller.fetchParameters(true);
    }
  }

  private DateBox datePicker;

  private Date parseDate(final String text)
  {
    try
    {
      return DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(text);
    }
    catch (Exception e)
    {
      // invalid date string ..
      Window.alert("Failed to parse date as date with timezone: " + text + " " + e);
    }
    try
    {
      return DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(text);
    }
    catch (Exception e)
    {
      // invalid date string ..
      Window.alert("Failed to parse date as date without timezone: " + text + " " + e);
    }

    try
    {
      // we use crippled dates as long as we have no safe and well-defined way to
      // pass date and time parameters from the server to the client and vice versa. we have to
      // parse the ISO-date the server supplies by default as date-only date-string.
      if (text.length() == 10)
      {
        try
        {
          return DateTimeFormat.getFormat("yyyy-MM-dd").parse(text);
        }
        catch (Exception e)
        {
          // invalid date string ..
          Window.alert("Failed to parse date as short-format: " + text + " " + e);
        }
      }
    }
    catch (Exception e)
    {
      // invalid date string ..
    }

    try
    {
      return new Date(Long.parseLong(text));
    }
    catch (Exception e)
    {
      // invalid number as well
      Window.alert("Failed to parse date as long-number: " + text);
    }
    return null;
  }

  public DateParameterUI(final ParameterControllerPanel controller,
                         final Parameter parameterElement)
  {
    // selectionsList should only have 1 date

    final List<ParameterSelection> list = parameterElement.getSelections();
    final Date date;
    if (list.isEmpty())
    {
      date = null;
    }
    else
    {
      final ParameterSelection parameterSelection = list.get(0);
      final String dateText = parameterSelection.getValue();
      date = parseDate(dateText);
    }
    final DefaultFormat format = new DefaultFormat(createFormat(parameterElement.getAttribute("data-format"))); // NON-NLS
    datePicker = new DateBox(new PentahoDatePicker(), date, format);

    final DateParameterSelectionHandler handler = new DateParameterSelectionHandler(controller, parameterElement);
    datePicker.getTextBox().addChangeHandler(handler);
    datePicker.addValueChangeHandler(handler);
    setWidget(datePicker);
  }

  private static DateTimeFormat createFormat(final String format)
  {
    if (format != null)
    {
      try
      {
        return DateTimeFormat.getFormat(format);
      }
      catch (Exception e)
      {
        // well, at least we tried ..
        Window.alert("Failed to recognize date-time-format:" + format + " " + e);
      }
    }
    return DateTimeFormat.getLongDateFormat();
  }

  public static native int getNativeTimezoneOffset(final double milliseconds)
    /*-{
      return (new Date(milliseconds).getTimezoneOffset());
    }-*/;
}
