package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.Date;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import org.pentaho.gwt.widgets.client.datepicker.PentahoDatePicker;

public class DateParameterUI extends SimplePanel
{
  private final static DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); // NON-NLS

  private class DateParameterSelectionHandler implements ValueChangeHandler<Date>, ChangeHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;

    public DateParameterSelectionHandler(final ParameterControllerPanel controller,
                                         final String parameterName)
    {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onValueChange(final ValueChangeEvent<Date> event)
    {
      final Date newDate = event.getValue();
      // add date as long
      controller.getParameterMap().setSelectedValue(parameterName, format.format(newDate)); //$NON-NLS-1$
      controller.fetchParameters(true);
    }

    public void onChange(final ChangeEvent changeEvent)
    {
      final String s = datePicker.getTextBox().getText();
      if (ReportViewerUtil.isEmpty(s))
      {
        controller.getParameterMap().setSelectedValue(parameterName, null); //$NON-NLS-1$
      }
      else
      {
        controller.getParameterMap().setSelectedValue(parameterName, format.format(datePicker.getValue())); //$NON-NLS-1$
      }
      controller.fetchParameters(true);
    }
  }

  private DateBox datePicker;

  private Date parseDate(final String text)
  {
    try
    {
      return format.parse(text);
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
      if (text.length() > 10)
      {
        return format.parse(text.substring(0, 10));
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

    final List<ParameterSelection> list = parameterElement.getSelections();
    final Date date;
    if (list.isEmpty())
    {
      date = null;
    }
    else
    {
      final ParameterSelection parameterSelection = list.get(0);
      date = parseDate(parameterSelection.getValue());
    }
    final DefaultFormat format = new DefaultFormat(createFormat(parameterElement.getAttribute("data-format"))); // NON-NLS
    datePicker = new DateBox(new PentahoDatePicker(), date, format);

    final DateParameterSelectionHandler handler = new DateParameterSelectionHandler(controller, parameterElement.getName());
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
      }
    }
    return DateTimeFormat.getLongDateFormat();
  }

}
