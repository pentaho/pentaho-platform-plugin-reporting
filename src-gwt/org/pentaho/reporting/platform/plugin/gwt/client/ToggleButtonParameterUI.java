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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ToggleButtonParameterUI extends SimplePanel implements ParameterUI {
  private class ToggleButtonParameterClickHandler implements ClickHandler {
    private ParameterControllerPanel controller;
    private String parameterName;
    private String choiceValue;
    private boolean multiSelect;
    private List<ToggleButton> buttonList;

    public ToggleButtonParameterClickHandler( final List<ToggleButton> buttonList,
        final ParameterControllerPanel controller, final String parameterName, final String choiceValue,
        final boolean multiSelect ) {
      this.controller = controller;
      this.parameterName = parameterName;
      this.choiceValue = choiceValue;
      this.multiSelect = multiSelect;
      this.buttonList = buttonList;
    }

    public void onClick( final ClickEvent event ) {
      final ToggleButton toggleButton = (ToggleButton) event.getSource();

      final ParameterValues parameterValues = controller.getParameterMap();
      // if we are single select buttons, we've got to clear the list
      if ( !multiSelect ) {
        parameterValues.setSelectedValue( parameterName, choiceValue );
        for ( final ToggleButton tb : buttonList ) {
          if ( toggleButton != tb ) {
            tb.setDown( false );
          }
        }
      } else {
        // remove element if it's already there (prevent dups for checkbox)
        parameterValues.removeSelectedValue( parameterName, choiceValue );
        if ( toggleButton.isDown() ) {
          parameterValues.addSelectedValue( parameterName, choiceValue );
        }
      }
      controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
    }
  }

  private ArrayList<ToggleButton> buttonList;

  public ToggleButtonParameterUI( final ParameterControllerPanel controller, final Parameter parameterElement ) {
    final String layout = parameterElement.getAttribute( "parameter-layout" ); //$NON-NLS-1$
    final boolean multiSelect = parameterElement.isMultiSelect(); //$NON-NLS-1$ //$NON-NLS-2$

    // build button ui
    final Panel buttonPanel;
    if ( "vertical".equals( layout ) ) { //$NON-NLS-1$
      buttonPanel = new VerticalPanel();
    } else {
      buttonPanel = new HorizontalPanel();
    }
    // need a button list so we can clear other selections for button-single mode
    buttonList = new ArrayList<ToggleButton>();
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for ( int i = 0; i < choices.size(); i++ ) {
      final ParameterSelection choiceElement = choices.get( i );
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      final ToggleButton toggleButton = new ToggleButton( choiceLabel );
      toggleButton.setStyleName( "pentaho-toggle-button" );
      if ( "vertical".equals( layout ) ) {
        toggleButton.addStyleName( "pentaho-toggle-button-vertical" );
      } else {
        toggleButton.addStyleName( "pentaho-toggle-button-horizontal" );
      }
      if ( i == 0 && choices.size() == 1 ) {
        toggleButton.addStyleName( "pentaho-toggle-button-single" );
      } else {
        if ( i == 0 ) {
          if ( "vertical".equals( layout ) ) {
            toggleButton.addStyleName( "pentaho-toggle-button-vertical-first" );
          } else {
            toggleButton.addStyleName( "pentaho-toggle-button-horizontal-first" );
          }
        }
        if ( i == choices.size() - 1 ) {
          if ( "vertical".equals( layout ) ) {
            toggleButton.addStyleName( "pentaho-toggle-button-vertical-last" );
          } else {
            toggleButton.addStyleName( "pentaho-toggle-button-horizontal-last" );
          }
        }
      }
      toggleButton.setTitle( choiceValue );
      toggleButton.setDown( choiceElement.isSelected() );
      buttonList.add( toggleButton );
      toggleButton.addClickHandler( new ToggleButtonParameterClickHandler( buttonList, controller, parameterElement
          .getName(), choiceValue, multiSelect ) );
      buttonPanel.add( toggleButton );
    }
    setWidget( buttonPanel );
  }

  public void setEnabled( final boolean enabled ) {
    for ( int i = 0; i < buttonList.size(); i++ ) {
      final ToggleButton button = buttonList.get( i );
      button.setEnabled( enabled );
    }
  }
}
