package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;

public class ListParameterUI extends SimplePanel
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

  public ListParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    final boolean multiSelect = parameterElement.isMultiSelect(); //$NON-NLS-1$ //$NON-NLS-2$

    final ListBox listBox = new ListBox(multiSelect);
    int visibleItems;
    final String visibleItemsStr = parameterElement.getAttribute("parameter-visible-items"); //$NON-NLS-1$
    try
    {
      visibleItems = Integer.parseInt(visibleItemsStr);
    }
    catch (Exception e)
    {
      visibleItems = 5;
    }

    listBox.setVisibleItemCount(visibleItems);
    final List<ParameterSelection> choices = parameterElement.getSelections();
    for (int i = 0; i < choices.size(); i++)
    {
      final ParameterSelection choiceElement = choices.get(i);
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      listBox.addItem(choiceLabel, choiceValue);
      final boolean selected = choiceElement.isSelected();
      listBox.setItemSelected(i, selected);
    }

    listBox.addChangeHandler(new ListBoxChangeHandler(controller, parameterElement.getName()));
    setWidget(listBox);
  }

}
