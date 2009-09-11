package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

public class ListParameterUI extends SimplePanel
{
  private class ListBoxChangeHandler implements ChangeHandler
  {
    private ParameterControllerPanel controller;
    private List<String> parameterSelections;

    public ListBoxChangeHandler(final List<String> parameterSelections, final ParameterControllerPanel controller)
    {
      this.controller = controller;
      this.parameterSelections = parameterSelections;
    }

    public void onChange(ChangeEvent event)
    {
      ListBox listBox = (ListBox) event.getSource();
      parameterSelections.clear();
      for (int i = 0; i < listBox.getItemCount(); i++)
      {
        if (listBox.isItemSelected(i))
        {
          parameterSelections.add(listBox.getValue(i));
        }
      }
      controller.fetchParameters(true);
    }
  }

  public ListParameterUI(final ParameterControllerPanel controller, final List<String> parameterSelections, final Element parameterElement)
  {
    String renderType = parameterElement.getAttribute("parameter-render-type"); //$NON-NLS-1$
    if (renderType != null)
    {
      renderType = renderType.trim();
    }

    boolean multiSelect = "true".equals(parameterElement.getAttribute("is-multi-select")); //$NON-NLS-1$ //$NON-NLS-2$

    final ListBox listBox = new ListBox(multiSelect);
    int visibleItems = 5;
    if ("dropdown".equalsIgnoreCase(renderType)) //$NON-NLS-1$
    {
      visibleItems = 1;
    }
    else
    {
      final String visibleItemsStr = parameterElement.getAttribute("parameter-visible-items"); //$NON-NLS-1$
      try
      {
        visibleItems = Integer.parseInt(visibleItemsStr);
      } catch (Exception e)
      {
        visibleItems = 5;
      }
    }
    listBox.setVisibleItemCount(visibleItems);

    boolean setAnything = false;
    NodeList choices = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
    for (int i = 0; i < choices.getLength(); i++)
    {
      final Element choiceElement = (Element) choices.item(i);
      final String choiceLabel = choiceElement.getAttribute("label"); //$NON-NLS-1$
      final String choiceValue = choiceElement.getAttribute("value"); //$NON-NLS-1$
      listBox.addItem(choiceLabel, choiceValue);
      for (String text : parameterSelections)
      {
        if (text.equals(choiceValue))
        {
          setAnything = true;
          listBox.setItemSelected(i, true);
          break;
        }
      }
    }

    // only force selection if we're using a 'drop-down' style
    if (visibleItems == 1 && setAnything == false)
    {
      parameterSelections.clear();
      if (listBox.getItemCount() > 0)
      {
        listBox.setItemSelected(0, true);
        parameterSelections.add(listBox.getValue(0));
      }
    }

    listBox.addChangeHandler(new ListBoxChangeHandler(parameterSelections, controller));
    setWidget(listBox);
  }

}
