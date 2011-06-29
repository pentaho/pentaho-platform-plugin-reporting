package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.gwt.widgets.client.listbox.CustomListBox;
import org.pentaho.gwt.widgets.client.listbox.DefaultListItem;
import org.pentaho.gwt.widgets.client.listbox.ListItem;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class DropDownParameterUI extends SimplePanel implements ParameterUI
{
  private class ListBoxChangeHandler implements ChangeListener
  {
    private ParameterControllerPanel controller;
    private String parameterName;

    public ListBoxChangeHandler(final ParameterControllerPanel controller,
                                final String parameterName)
    {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onChange(Widget sender) {
      updateSelection((CustomListBox)sender);
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }
    
    public void updateSelection(CustomListBox listBox) {
      final ArrayList<String> selectedItems = new ArrayList<String>();
      for (ListItem item : listBox.getSelectedItems()) {
        selectedItems.add((String)item.getValue());
      }
      controller.getParameterMap().setSelectedValues
          (parameterName, selectedItems.toArray(new String[selectedItems.size()]));
    }
  }

  private CustomListBox listBox;

  public DropDownParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    listBox = new CustomListBox();
    listBox.setMultiSelect(false);
    listBox.setVisibleRowCount(1);

    boolean hasSelection = false;
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for (int i = 0; i < choices.size(); i++)
    {
      final ParameterSelection choiceElement = choices.get(i);
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      
      DefaultListItem item = new DefaultListItem(choiceLabel);
      item.setValue(choiceValue);

      listBox.addItem(item);
      final boolean selected = choiceElement.isSelected();
      if (selected)
      {
        listBox.setSelectedIndex(i);
        hasSelection = true;
      }
    }

    ListBoxChangeHandler lbChangeHandler = new ListBoxChangeHandler(controller, parameterElement.getName());
    listBox.addChangeListener(lbChangeHandler);

    if (hasSelection == false) {
      listBox.setSelectedIndex(0);
    }
    
    lbChangeHandler.updateSelection(listBox);
    
    setWidget(listBox);
  }

  public void setEnabled(final boolean enabled)
  {
    listBox.setEnabled(enabled); 
  }
}
