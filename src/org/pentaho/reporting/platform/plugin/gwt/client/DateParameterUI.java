package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.Date;
import java.util.List;

import org.pentaho.gwt.widgets.client.datepicker.PentahoDatePicker;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.xml.client.Element;

public class DateParameterUI extends SimplePanel
{
  private final static DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd"); //$NON-NLS-1$

  private class DateParameterSelectionHandler implements ValueChangeHandler<Date>
  {
    private List<String> parameterSelections;
    private ParameterControllerPanel controller;

    public DateParameterSelectionHandler(final List<String> parameterSelections,
                                         final ParameterControllerPanel controller)
    {
      this.parameterSelections = parameterSelections;
      this.controller = controller;
    }

    public void onValueChange(ValueChangeEvent<Date> event)
    {
      parameterSelections.clear();
      Date newDate = event.getValue();
      // add date as long
      parameterSelections.add(format.format(newDate)); //$NON-NLS-1$
      controller.fetchParameters(true);
    }

  }

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
                         final List<String> parameterSelections,
                         final Element parameterElement)
  {
    // selectionsList should only have 1 date
    final Date date;
    if (parameterSelections.size() > 0)
    {
      final String paramAsText = parameterSelections.get(0);
      date = parseDate(paramAsText);
    }
    else
    {
      // BISERVER-4090: We do only ignore the default now() date, but if the user
      // specified a date via a formula, then they get what they specified, as ignoring User-input
      // is never a sane option.
      if ("true".equals(Window.Location.getParameter("ignoreDefaultDates"))) //$NON-NLS-1$ //$NON-NLS-2$
      {
        date = null;
      }
      else
      {
        date = new Date();
      }
    }
    if (date == null)
    {
      // add the current date as the default, otherwise, a submission of another parameter
      // will not result in this parameter being submitted
      parameterSelections.clear();
      parameterSelections.add(""); //$NON-NLS-1$
    }
    else
    {
      // This normalizes the date. No matter how the user specified it, we will now always have
      // a long number in our selection.
      parameterSelections.clear();
      parameterSelections.add(format.format(date)); //$NON-NLS-1$
    }
    
    final DefaultFormat format = new DefaultFormat(createFormat(parameterElement.getAttribute("data-format"))); //$NON-NLS-1$
    final DateBox datePicker = new DateBox(new PentahoDatePicker(), date, format);

    datePicker.addValueChangeHandler(new DateParameterSelectionHandler(parameterSelections, controller));
    setWidget(datePicker);
  }

  private DateTimeFormat createFormat(final String format)
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
