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

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.pentaho.gwt.widgets.client.listbox.CustomListBox;
import org.pentaho.gwt.widgets.client.listbox.DefaultListItem;
import org.pentaho.gwt.widgets.client.listbox.ListItem;

public class DropDownParameterUI extends SimplePanel implements ParameterUI {
  private class ListBoxChangeHandler implements ChangeListener {
    private ParameterControllerPanel controller;
    private String parameterName;

    public ListBoxChangeHandler( final ParameterControllerPanel controller, final String parameterName ) {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onChange( final Widget sender ) {
      updateSelection( (CustomListBox) sender );
      controller.fetchParameters( ParameterControllerPanel.ParameterSubmitMode.USERINPUT );
    }

    public void updateSelection( final CustomListBox listBox ) {
      final ArrayList<String> selectedItems = new ArrayList<String>();
      for ( final ListItem item : listBox.getSelectedItems() ) {
        selectedItems.add( (String) item.getValue() );
      }
      controller.getParameterMap().setSelectedValues( parameterName,
          selectedItems.toArray( new String[selectedItems.size()] ) );
    }
  }

  private CustomListBox listBox;

  public DropDownParameterUI( final ParameterControllerPanel controller, final ParameterDefinition parameterDefinition,
      final Parameter parameterElement ) {
    listBox = new CustomListBox();
    listBox.setMultiSelect( false );
    listBox.setVisibleRowCount( 1 );

    boolean hasSelection = false;
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for ( int i = 0; i < choices.size(); i++ ) {
      final ParameterSelection choiceElement = choices.get( i );
      if ( choiceElement.isSelected() ) {
        hasSelection = true;
        break;
      }
    }

    if ( parameterDefinition.isIgnoreBiServer5538() ) {
      // If there is no empty selection, and no value is selected, create one. This way, we can represent
      // the unselected state.
      if ( hasSelection == false ) {
        final DefaultListItem item = new DefaultListItem( " " );
        item.setValue( null );
        listBox.addItem( item );
        listBox.setSelectedIndex( 0 );
      }
    }

    for ( int i = 0; i < choices.size(); i++ ) {
      final ParameterSelection choiceElement = choices.get( i );
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$

      final DefaultListItem item = new DefaultListItem( choiceLabel );
      item.setValue( choiceValue );

      listBox.addItem( item );
      final boolean selected = choiceElement.isSelected();
      if ( selected ) {
        listBox.setSelectedIndex( i );
      }
    }

    final ListBoxChangeHandler lbChangeHandler = new ListBoxChangeHandler( controller, parameterElement.getName() );
    listBox.addChangeListener( lbChangeHandler );
    listBox.setTableLayout( "auto" );

    if ( parameterDefinition.isIgnoreBiServer5538() == false ) {
      // This sort of magic invalidates the parameter calculation on the server and shows a bogus
      // error message to the user when the server complains about a invalid or missing value while
      // we silently select the first value.

      // The reporting plugin now contains a local override that disables this fix. Dashboards and
      // all other users of the parameter UI may proceed with their magic show.
      if ( hasSelection == false ) {
        listBox.setSelectedIndex( 0 );
      }
    }

    lbChangeHandler.updateSelection( listBox );

    setWidget( listBox );
  }

  public void setEnabled( final boolean enabled ) {
    listBox.setEnabled( enabled );
  }
}
