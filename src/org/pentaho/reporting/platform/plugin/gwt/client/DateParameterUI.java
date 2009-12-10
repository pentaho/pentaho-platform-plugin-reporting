package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.Date;
import java.util.List;

import org.pentaho.gwt.widgets.client.datepicker.PentahoDatePicker;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.xml.client.Element;

public class DateParameterUI extends SimplePanel
{

  private class DateParameterSelectionHandler implements ValueChangeHandler<Date>
  {
    private List<String> parameterSelections;
    private ParameterControllerPanel controller;

    public DateParameterSelectionHandler(final List<String> parameterSelections, final ParameterControllerPanel controller)
    {
      this.parameterSelections = parameterSelections;
      this.controller = controller;
    }

    public void onValueChange(ValueChangeEvent<Date> event)
    {
      parameterSelections.clear();
      Date newDate = event.getValue();
      // add date as long
      parameterSelections.add("" + newDate.getTime()); //$NON-NLS-1$
      controller.fetchParameters(true);
    }

  }

  private Date parseDate(final String text)
  {
    try
    {
      return new Date(Long.parseLong(text));
    }
    catch (Exception e)
    {
      // invalid number as well
    }
    try
    {
      final DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      return format.parse(text);
    }
    catch (Exception e)
    {
      // invalid date string ..
    }
    return new Date();
  }

  public DateParameterUI(final ParameterControllerPanel controller,
                         final List<String> parameterSelections,
                         final Element parameterElement)
  {
    // selectionsList should only have 1 date
    Date date = new Date();
    if (parameterSelections.size() > 0)
    {
      final String paramAsText = parameterSelections.get(0);
      date = parseDate(paramAsText);
    }
    else
    {
      // add the current date as the default, otherwise, a submission of another parameter
      // will not result in this parameter being submitted
      parameterSelections.clear();
      parameterSelections.add("" + date.getTime()); //$NON-NLS-1$
    }

    final DefaultFormat format = new DefaultFormat(createFormat(parameterElement.getAttribute("data-format")));
    final DateBox datePicker = new DateBox(new PentahoDatePicker(), date, format);

    datePicker.addValueChangeHandler(new DateParameterSelectionHandler(parameterSelections, controller));
    setWidget(datePicker);
  }

  private DateTimeFormat createFormat(final String format)
  {
    if (format != null)
    try
    {
      return DateTimeFormat.getFormat(format);
    }
    catch (Exception e)
    {
      // well, at least we tried ..
    }
    return DateTimeFormat.getLongDateFormat();
  }

}
