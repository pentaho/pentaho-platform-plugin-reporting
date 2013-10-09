/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

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

public class DateParameterUI extends SimplePanel implements ParameterUI {
  protected class DateParameterSelectionHandler implements ValueChangeHandler<Date>, ChangeHandler {
    private ParameterControllerPanel controller;
    private String parameterName;
    private DateTimeFormat format;
    private boolean waitForNext;

    public DateParameterSelectionHandler( final ParameterControllerPanel controller, final Parameter parameter ) {
      this.controller = controller;
      this.parameterName = parameter.getName();
      this.format = ReportViewerUtil.createDateTransportFormat( parameter );
    }

    public void onValueChange( final ValueChangeEvent<Date> event ) {
      if ( waitForNext == false ) {
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
      final String value = convertSelectionToText( newDate );

      controller.getParameterMap().setSelectedValue( parameterName, value );
      controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
    }

    protected String convertSelectionToText( final Date newDate ) {
      if ( newDate == null ) {
        return null; //$NON-NLS-1$
      }

      return format.format( newDate );
    }

    public void onChange( final ChangeEvent changeEvent ) {
      final String s = datePicker.getTextBox().getText();
      if ( ReportViewerUtil.isEmpty( s ) ) {
        controller.getParameterMap().setSelectedValue( parameterName, null );
        controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
      }
    }
  }

  private DateBox datePicker;
  private DateParameterSelectionHandler selectionHandler;

  public DateParameterUI( final ParameterControllerPanel controller, final Parameter parameterElement ) {
    // selectionsList should only have 1 date

    final List<ParameterSelection> list = parameterElement.getSelections();
    final Date date;
    if ( list.isEmpty() ) {
      date = null;
    } else {
      final ParameterSelection parameterSelection = list.get( 0 );
      final String dateText = parameterSelection.getValue();
      date = ReportViewerUtil.parseDate( parameterElement, dateText );
    }

    final DefaultFormat format =
       new DefaultFormat( createFormat( parameterElement.getAttribute( "data-format" ) ) ); // NON-NLS
    datePicker = new DateBox( new PentahoDatePicker(), date, format );
    datePicker.getElement().setAttribute( "paramType", "date" );

    selectionHandler = new DateParameterSelectionHandler( controller, parameterElement );
    datePicker.getTextBox().addChangeHandler( selectionHandler );
    datePicker.addValueChangeHandler( selectionHandler );
    setWidget( datePicker );
  }

  public void setEnabled( final boolean enabled ) {
    datePicker.setEnabled( enabled );
  }

  protected DateParameterSelectionHandler getSelectionHandler() {
    return selectionHandler;
  }

  private static DateTimeFormat createFormat( final String format ) {
    if ( format != null ) {
      try {
        return DateTimeFormat.getFormat( format );
      } catch ( Exception e ) {
        // well, at least we tried ..
        Window.alert( "Failed to recognize date-time-format:" + format + " " + e );
      }
    }
    return DateTimeFormat.getLongDateFormat();
  }

}
