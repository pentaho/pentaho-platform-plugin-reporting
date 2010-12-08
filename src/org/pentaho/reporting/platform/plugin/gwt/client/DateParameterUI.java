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

public class DateParameterUI extends SimplePanel implements ParameterUI
{
  protected class DateParameterSelectionHandler implements ValueChangeHandler<Date>, ChangeHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;
    private String timezone;
    private DateTimeFormat format;
    private boolean waitForNext;

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
      if (waitForNext == false)
      {
        // GWT fires date change events twice and the first date is absolutely wrong with a offset 12 hours
        // in the future. The code for the date-picker looks creepy, as they try to keep things in sync between
        // the messagebox (and there they parse the date) and the date-picker itself.
        //
        // Both firefox and safari show this problem, probably IE and other browsers as well.
        waitForNext = true;
        return;
      }

      waitForNext = false;
      final Date newDate = event.getValue();
      final String value = convertSelectionToText(newDate);


      controller.getParameterMap().setSelectedValue(parameterName, value);
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }

    protected String convertSelectionToText(final Date newDate)
    {
      if (newDate == null)
      {
        return null; //$NON-NLS-1$
      }

      return format.format(newDate);
    }

    public void onChange(final ChangeEvent changeEvent)
    {
      final String s = datePicker.getTextBox().getText();
      if (ReportViewerUtil.isEmpty(s))
      {
        controller.getParameterMap().setSelectedValue(parameterName, null);
        controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
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

  public void setEnabled(final boolean enabled)
  {
    datePicker.setEnabled(enabled);
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
