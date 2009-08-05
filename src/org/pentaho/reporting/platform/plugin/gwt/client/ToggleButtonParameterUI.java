package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

public class ToggleButtonParameterUI extends SimplePanel
{
  private class ToggleButtonParameterClickHandler implements ClickHandler
  {
    private ParameterControllerPanel controller;
    private String choiceValue;
    private boolean multiSelect;
    private List<String> parameterSelections;
    private List<ToggleButton> buttonList;

    public ToggleButtonParameterClickHandler(final List<String> parameterSelections, List<ToggleButton> buttonList, final ParameterControllerPanel controller,
        final String choiceValue, final boolean multiSelect)
    {
      this.controller = controller;
      this.choiceValue = choiceValue;
      this.multiSelect = multiSelect;
      this.parameterSelections = parameterSelections;
      this.buttonList = buttonList;
    }

    public void onClick(ClickEvent event)
    {
      ToggleButton toggleButton = (ToggleButton) event.getSource();

      // if we are single select buttons, we've got to clear the list
      if (!multiSelect)
      {
        parameterSelections.clear();
        for (ToggleButton tb : buttonList)
        {
          if (toggleButton != tb)
          {
            tb.setDown(false);
          }
        }
      }
      else
      {
        // remove element if it's already there (prevent dups for checkbox)
        parameterSelections.remove(choiceValue);
      }
      if (toggleButton.isDown())
      {
        parameterSelections.add(choiceValue);
      }
      controller.fetchParameters(true);
    }
  }

  public ToggleButtonParameterUI(final ParameterControllerPanel controller, final List<String> parameterSelections,
      final Element parameterElement)
  {
    String renderType = parameterElement.getAttribute("parameter-render-type");
    if (renderType != null)
    {
      renderType = renderType.trim();
    }
    String layout = parameterElement.getAttribute("parameter-layout");
    if (layout != null)
    {
      layout = layout.trim();
    }
    boolean multiSelect = "true".equals(parameterElement.getAttribute("is-multi-select"));

    // build button ui
    CellPanel buttonPanel = null;
    if ("vertical".equalsIgnoreCase(layout))
    {
      buttonPanel = new VerticalPanel();
    }
    else
    {
      buttonPanel = new HorizontalPanel();
    }
    // need a button list so we can clear other selections for button-single mode
    final List<ToggleButton> buttonList = new ArrayList<ToggleButton>();
    NodeList choices = parameterElement.getElementsByTagName("value-choice");
    for (int i = 0; i < choices.getLength(); i++)
    {
      final Element choiceElement = (Element) choices.item(i);
      final String choiceLabel = choiceElement.getAttribute("label");
      final String choiceValue = choiceElement.getAttribute("value");
      final ToggleButton toggleButton = new ToggleButton(choiceLabel);
      toggleButton.setTitle(choiceValue);
      if (parameterSelections.contains(choiceValue))
      {
        toggleButton.setDown(true);
      }
      buttonList.add(toggleButton);
      toggleButton.addClickHandler(new ToggleButtonParameterClickHandler(parameterSelections, buttonList, controller, choiceValue, multiSelect));
      buttonPanel.add(toggleButton);
    }
    setWidget(buttonPanel);
  }

}
