package org.pentaho.reporting.platform.plugin.gwt.client;

import com.google.gwt.user.datepicker.client.CalendarModel;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.user.datepicker.client.DefaultCalendarView;

public class MyDatePicker extends DatePicker {

  public MyDatePicker() {
    super(new MyMonthSelector(), new DefaultCalendarView(), new CalendarModel());
    ((MyMonthSelector) getMonthSelector()).setMyDatePicker(this);
  }

  public void addMonths(int numMonths) {
    getModel().shiftCurrentMonth(numMonths);
    refreshAll();
  }

}
