package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CheckBoxParameterUI extends SimplePanel implements ParameterUI
{

  private class CheckBoxParameterClickHandler implements ClickHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;
    private String choiceValue;

    public CheckBoxParameterClickHandler(final ParameterControllerPanel controller,
                                         final String parameterName,
                                         final String choiceValue)
    {
      this.controller = controller;
      this.parameterName = parameterName;
      this.choiceValue = choiceValue;
    }

    public void onClick(final ClickEvent event)
    {
      final CheckBox button = (CheckBox) event.getSource();
      // if we are render radio buttons, we've got to clear the list
      // remove element if it's already there (prevent dups for checkbox)
      final ParameterValues parameterValues = controller.getParameterMap();
      parameterValues.removeSelectedValue(parameterName, choiceValue);

      if (button.getValue())
      {
        parameterValues.addSelectedValue(parameterName, choiceValue);
      }
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }
  }


  private class RadioButtonParameterClickHandler implements ClickHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;
    private String choiceValue;

    public RadioButtonParameterClickHandler(final ParameterControllerPanel controller,
                                         final String parameterName,
                                         final String choiceValue)
    {
      this.controller = controller;
      this.parameterName = parameterName;
      this.choiceValue = choiceValue;
    }

    public void onClick(final ClickEvent event)
    {
      final CheckBox button = (CheckBox) event.getSource();
      // if we are render radio buttons, we've got to clear the list
      if (button.getValue())
      {
        controller.getParameterMap().setSelectedValue(parameterName, choiceValue);
      }
      else
      {
        controller.getParameterMap().setSelectedValue(parameterName, null);
      }
      controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
    }
  }

  private ArrayList<CheckBox> buttons;

  public CheckBoxParameterUI(final ParameterControllerPanel controller,
                             final Parameter parameterElement)
  {
    buttons = new ArrayList<CheckBox>();
    final String parameterName = parameterElement.getName(); //$NON-NLS-1$
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
    final CellPanel buttonPanel;
    if ("vertical".equalsIgnoreCase(layout)) //$NON-NLS-1$
    {
      buttonPanel = new VerticalPanel();
    }
    else
    {
      buttonPanel = new HorizontalPanel();
    }
    final List<ParameterSelection> selections = parameterElement.getSelections();
    for (int i = 0; i < selections.size(); i++)
    {
      final ParameterSelection choiceElement = selections.get(i);
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      final CheckBox tmpButton;
      if ("checkbox".equals(renderType)) //$NON-NLS-1$
      {
        tmpButton = new CheckBox(choiceLabel);
        tmpButton.addClickHandler(new CheckBoxParameterClickHandler(controller, parameterName, choiceValue));
      }
      else
      {
        tmpButton = new RadioButton(parameterName, choiceLabel);
        tmpButton.addClickHandler(new RadioButtonParameterClickHandler(controller, parameterName, choiceValue));
      }
      tmpButton.setValue(choiceElement.isSelected());
      tmpButton.setTitle(choiceValue);
      // set checked based on selections list
      buttonPanel.add(tmpButton);
      buttons.add(tmpButton);
    }
    setWidget(buttonPanel);
  }

  public void setEnabled(final boolean enabled)
  {
    for (int i = 0; i < buttons.size(); i++)
    {
      final CheckBox checkBox = buttons.get(i);
      checkBox.setEnabled(enabled);
    }
  }
}
