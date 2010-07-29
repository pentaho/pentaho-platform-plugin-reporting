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
  protected class DateParameterSelectionHandler implements ValueChangeHandler<Date>, ChangeHandler
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
      if ("client".equals(timezone))
      {
        format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      }
      else
      {
        // Take the date string as it comes from the server, cut out the timezone information - the
        // server will supply its own here.
        if (timezoneHint != null && timezoneHint.length() > 0)
        {
          format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS" + "'" + timezoneHint + "'");
        }
        else
        {
          if ("server".equals(timezone) || timezone == null)
          {
            format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
          }
          else if ("utc".equals(timezone))
          {
            format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'");
          }
          else if (timezone != null)
          {
            format = DateTimeFormat.getFormat
                ("yyyy-MM-dd'T'HH:mm:ss.SSS'" + TimeZoneOffsets.getInstance().getOffsetAsString(timezone) + "'");
          }
        }
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
      final String value = convertSelectionToText(newDate);


      controller.getParameterMap().setSelectedValue(parameterName, value);
      controller.fetchParameters(true);
    }

    protected String convertSelectionToText(final Date newDate)
    {
      // add date as long
      final String value;
      if (newDate == null)
      {
        value = null; //$NON-NLS-1$
      }
      else
      {
        value = format.format(newDate);
      }
      return value;
    }

    public void onChange(final ChangeEvent changeEvent)
    {
      final String s = datePicker.getTextBox().getText();
      if (ReportViewerUtil.isEmpty(s))
      {
        datePicker.setValue(null, true);
      }
    }
  }

  private DateBox datePicker;
  private DateParameterSelectionHandler selectionHandler;

  private String timezoneMode;

  protected Date parseDate(final String text)
  {
    if ("client".equals(timezoneMode))
    {
      try
      {
        return ReportViewerUtil.parseWithTimezone(text);
      }
      catch (Exception e)
      {
        // invalid date string ..
      }
    }

    try
    {
      return ReportViewerUtil.parseWithoutTimezone(text);
    }
    catch (Exception e)
    {
      // invalid date string ..
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
    }
    return null;
  }

  public DateParameterUI(final ParameterControllerPanel controller,
                         final Parameter parameterElement)
  {
    // selectionsList should only have 1 date

    this.timezoneMode = parameterElement.getAttribute("timezone");

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

    selectionHandler = new DateParameterSelectionHandler(controller, parameterElement);
    datePicker.getTextBox().addChangeHandler(selectionHandler);
    datePicker.addValueChangeHandler(selectionHandler);
    setWidget(datePicker);
  }

  protected DateParameterSelectionHandler getSelectionHandler()
  {
    return selectionHandler;
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

}
