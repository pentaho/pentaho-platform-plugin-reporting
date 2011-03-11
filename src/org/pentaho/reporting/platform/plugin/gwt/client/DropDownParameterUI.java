package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;

public class DropDownParameterUI extends SimplePanel implements ParameterUI
{
  private class ListBoxChangeHandler implements ChangeHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;

    public ListBoxChangeHandler(final ParameterControllerPanel controller,
                                final String parameterName)
    {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onChange(final ChangeEvent event)
    {
      updateSelection((ListBox) event.getSource());
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }
    
    public void updateSelection(ListBox listBox) {
      final ArrayList<String> selectedItems = new ArrayList<String>();
      for (int i = 0; i < listBox.getItemCount(); i++)
      {
        if (listBox.isItemSelected(i))
        {
          selectedItems.add(values.get(i));
        }
      }
      controller.getParameterMap().setSelectedValues
          (parameterName, selectedItems.toArray(new String[selectedItems.size()]));
    }
  }

  private ListBox listBox;
  private ArrayList<String> values;

  public DropDownParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    listBox = new ListBox(false);
    listBox.setVisibleItemCount(1);
    values = new ArrayList<String>();

    boolean hasSelection = false;
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for (int i = 0; i < choices.size(); i++)
    {
      final ParameterSelection choiceElement = choices.get(i);
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      listBox.addItem(choiceLabel, String.valueOf(i));
      values.add(choiceValue);
      final boolean selected = choiceElement.isSelected();
      listBox.setItemSelected(i, selected);
      if (selected)
      {
        hasSelection = true;
      }
    }

    ListBoxChangeHandler lbChangeHandler = new ListBoxChangeHandler(controller, parameterElement.getName());
    listBox.addChangeHandler(lbChangeHandler);

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
