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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;

public class ListParameterUI extends SimplePanel implements ParameterUI {
  private class ListBoxChangeHandler implements ChangeHandler {
    private ParameterControllerPanel controller;
    private String parameterName;

    public ListBoxChangeHandler( final ParameterControllerPanel controller, final String parameterName ) {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onChange( final ChangeEvent event ) {
      final ListBox listBox = (ListBox) event.getSource();
      final ArrayList<String> selectedItems = new ArrayList<String>();
      for ( int i = 0; i < listBox.getItemCount(); i++ ) {
        if ( listBox.isItemSelected( i ) ) {
          selectedItems.add( values.get( i ) );
        }
      }
      controller.getParameterMap().setSelectedValues( parameterName,
          selectedItems.toArray( new String[selectedItems.size()] ) );
      controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
    }
  }

  private ListBox listBox;
  private ArrayList<String> values;

  public ListParameterUI( final ParameterControllerPanel controller, final Parameter parameterElement ) {
    final boolean multiSelect = parameterElement.isMultiSelect(); //$NON-NLS-1$ //$NON-NLS-2$

    listBox = new ListBox( multiSelect );
    values = new ArrayList<String>();

    int visibleItems;
    final String visibleItemsStr = parameterElement.getAttribute( "parameter-visible-items" ); //$NON-NLS-1$
    try {
      visibleItems = Integer.parseInt( visibleItemsStr );
    } catch ( Exception e ) {
      visibleItems = 5;
    }

    listBox.setVisibleItemCount( visibleItems );
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for ( int i = 0; i < choices.size(); i++ ) {
      final ParameterSelection choiceElement = choices.get( i );
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      listBox.addItem( choiceLabel, String.valueOf( i ) );
      values.add( choiceValue );
      final boolean selected = choiceElement.isSelected();
      listBox.setItemSelected( i, selected );
    }

    listBox.addChangeHandler( new ListBoxChangeHandler( controller, parameterElement.getName() ) );
    setWidget( listBox );
  }

  public void setEnabled( final boolean enabled ) {
    listBox.setEnabled( enabled );
  }
}
