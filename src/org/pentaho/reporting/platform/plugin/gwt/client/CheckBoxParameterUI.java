package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

public class CheckBoxParameterUI extends SimplePanel
{

  private class ButtonParameterClickHandler implements ClickHandler
  {
    private ParameterControllerPanel controller;
    private String choiceValue;
    private String renderType;
    private List<String> parameterSelections;

    public ButtonParameterClickHandler(final List<String> parameterSelections, final ParameterControllerPanel controller, final String choiceValue,
        final String renderType)
    {
      this.controller = controller;
      this.choiceValue = choiceValue;
      this.renderType = renderType;
      this.parameterSelections = parameterSelections;
    }

    public void onClick(ClickEvent event)
    {
      CheckBox button = (CheckBox) event.getSource();
      // if we are render radio buttons, we've got to clear the list
      if ("radio".equalsIgnoreCase(renderType)) //$NON-NLS-1$
      {
        parameterSelections.clear();
      }
      else
      {
        // remove element if it's already there (prevent dups for checkbox)
        parameterSelections.remove(choiceValue);
      }
      if (button.getValue())
      {
        parameterSelections.add(choiceValue);
      }
      controller.fetchParameters(true);
    }
  }

  public CheckBoxParameterUI(final ParameterControllerPanel controller, final List<String> parameterSelections, final Element parameterElement)
  {
    final String parameterName = parameterElement.getAttribute("name"); //$NON-NLS-1$
    String renderType = parameterElement.getAttribute("parameter-render-type"); //$NON-NLS-1$
    if (renderType != null)
    {
      renderType = renderType.trim();
    }
    String layout = parameterElement.getAttribute("parameter-layout"); //$NON-NLS-1$
    if (layout != null)
    {
      layout = layout.trim();
    }

    // build button ui
    CellPanel buttonPanel = null;
    if ("vertical".equalsIgnoreCase(layout)) //$NON-NLS-1$
    {
      buttonPanel = new VerticalPanel();
    }
    else
    {
      buttonPanel = new HorizontalPanel();
    }
    NodeList choices = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
    for (int i = 0; i < choices.getLength(); i++)
    {
      final Element choiceElement = (Element) choices.item(i);
      final String choiceLabel = choiceElement.getAttribute("label"); //$NON-NLS-1$
      final String choiceValue = choiceElement.getAttribute("value"); //$NON-NLS-1$
      CheckBox tmpButton = new RadioButton(parameterName, choiceLabel);
      if ("checkbox".equalsIgnoreCase(renderType)) //$NON-NLS-1$
      {
        tmpButton = new CheckBox(choiceLabel);
      }
      if (parameterSelections.contains(choiceValue))
      {
        tmpButton.setValue(true);
      }
      final CheckBox button = tmpButton;
      button.setTitle(choiceValue);
      // set checked based on selections list
      button.addClickHandler(new ButtonParameterClickHandler(parameterSelections, controller, choiceValue, renderType));
      buttonPanel.add(button);
    }
    setWidget(buttonPanel);
  }

}
