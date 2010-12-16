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
      final ListBox listBox = (ListBox) event.getSource();
      final ArrayList<String> selectedItems = new ArrayList<String>();
      for (int i = 0; i < listBox.getItemCount(); i++)
      {
        if (listBox.isItemSelected(i))
        {
          selectedItems.add(listBox.getValue(i));
        }
      }
      controller.getParameterMap().setSelectedValues
          (parameterName, selectedItems.toArray(new String[selectedItems.size()]));
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }
  }

  private ListBox listBox;

  public DropDownParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    listBox = new ListBox(false);
    listBox.setVisibleItemCount(1);

    boolean hasSelection = false;
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for (int i = 0; i < choices.size(); i++)
    {
      final ParameterSelection choiceElement = choices.get(i);
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      listBox.addItem(choiceLabel, choiceValue);
      final boolean selected = choiceElement.isSelected();
      listBox.setItemSelected(i, selected);
      if (selected)
      {
        hasSelection = true;
      }
    }

    if (hasSelection == false)
    {
      listBox.setSelectedIndex(-1);
    }

    listBox.addChangeHandler(new ListBoxChangeHandler(controller, parameterElement.getName()));
    setWidget(listBox);
  }

  public void setEnabled(final boolean enabled)
  {
    listBox.setEnabled(enabled); 
  }
}
