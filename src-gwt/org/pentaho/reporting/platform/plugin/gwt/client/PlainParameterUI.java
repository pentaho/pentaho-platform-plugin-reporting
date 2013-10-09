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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class PlainParameterUI extends SimplePanel implements ParameterUI {
  private final Map<String, String> labelToValueMap = new HashMap<String, String>();

  private class PlainParameterKeyUpHandler implements KeyUpHandler {
    private ParameterControllerPanel controller;
    private String parameterName;

    public PlainParameterKeyUpHandler( final ParameterControllerPanel controller, final String parameterName ) {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onKeyUp( final KeyUpEvent event ) {
      if ( listParameter ) {
        final SuggestBox textBox = (SuggestBox) event.getSource();
        final String text = textBox.getText();
        String value = labelToValueMap.get( text );
        if ( value == null ) {
          // If the input cannot be mapped into a valid value from the server, send the server the raw input.
          // The server will reject the input if the parameter is strictly validating, but unless a replacement input
          // gets calculated on the server side, we will get the invalid input back so that the user can correct it.
          value = text;
        }

        controller.getParameterMap().setSelectedValue( parameterName, value );
      } else if ( dataFormat != null ) {
        try {
          textBox.getTextBox().setStyleName( "" );
          final String transportObject =
              ReportViewerUtil.createTransportObject( parameterElement, dataFormat.parse( textBox.getText() ) );
          controller.getParameterMap().setSelectedValue( parameterName, transportObject );
        } catch ( Exception e ) {
          textBox.getTextBox().setStyleName( "text-parse-error" );
          // ignore partial values ..
          // controller.getParameterMap().setSelectedValue(null, value);
        }
      } else {
        controller.getParameterMap().setSelectedValue( parameterName, textBox.getText() );
      }

      if ( event.getNativeKeyCode() == KeyCodes.KEY_ENTER ) {
        // on enter, force update
        controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
      }
    }

  }

  private class PlainParameterSelectionHandler implements SelectionHandler<Suggestion> {
    private ParameterControllerPanel controller;
    private String parameterName;

    public PlainParameterSelectionHandler( final ParameterControllerPanel controller, final String parameterName ) {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onSelection( final SelectionEvent<Suggestion> event ) {
      final String text = event.getSelectedItem().getReplacementString();
      String value = labelToValueMap.get( text );
      if ( value == null ) {
        value = text;
      }
      if ( ReportViewerUtil.isEmpty( value ) ) {
        controller.getParameterMap().setSelectedValue( parameterName, null );
      } else {
        controller.getParameterMap().setSelectedValue( parameterName, value );
      }
      controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
    }

  }

  private SuggestBox textBox;
  private boolean listParameter;
  private boolean strict;
  private TextFormat dataFormat;
  private Parameter parameterElement;

  public PlainParameterUI( final ParameterControllerPanel controller, final Parameter parameterElement ) {
    this.parameterElement = parameterElement;
    final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    final List<ParameterSelection> selections = parameterElement.getSelections();
    for ( int i = 0; i < selections.size(); i++ ) {
      final ParameterSelection choiceElement = selections.get( i );
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      if ( choiceLabel != null && choiceLabel.length() > 0 ) {
        oracle.add( choiceLabel );
        labelToValueMap.put( choiceLabel, choiceValue );
      }
    }

    strict = parameterElement.isStrict();
    listParameter = parameterElement.isList();
    textBox = new SuggestBox( oracle );

    final String dataType = parameterElement.getType();
    if ( parameterElement.isList() ) {
      // formatting and lists are mutually exclusive.
      dataFormat = null;
    } else {
      final String dataFormatText = parameterElement.getAttribute( "data-format" );
      dataFormat = ReportViewerUtil.createTextFormat( dataFormatText, dataType );
    }

    if ( selections.isEmpty() ) {
      textBox.setText( "" ); //$NON-NLS-1$
    } else {
      ParameterSelection parameterSelection = null;
      for ( int i = 0; i < selections.size(); i++ ) {
        final ParameterSelection selection = selections.get( i );
        if ( selection.isSelected() ) {
          parameterSelection = selection;
        }
      }

      if ( parameterSelection != null ) {
        final String labelText = parameterSelection.getLabel();
        if ( dataFormat != null ) {
          final Object rawObject = ReportViewerUtil.createRawObject( labelText, parameterElement );
          if ( rawObject != null ) {
            textBox.setText( dataFormat.format( rawObject ) );
          } else {
            textBox.setText( labelText );
          }
        } else {
          textBox.setText( labelText );
        }
      }
    }

    textBox.addSelectionHandler( new PlainParameterSelectionHandler( controller, parameterElement.getName() ) );
    textBox.addKeyUpHandler( new PlainParameterKeyUpHandler( controller, parameterElement.getName() ) );
    setWidget( textBox );
  }

  public void setEnabled( final boolean enabled ) {
    textBox.getTextBox().setEnabled( enabled );
  }
}
