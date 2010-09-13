package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class PlainParameterUI extends SimplePanel
{
  private final Map<String, String> labelToValueMap = new HashMap<String, String>();

  private class PlainParameterKeyUpHandler implements KeyUpHandler
  {
    private ParameterControllerPanel controller;
    private String parameterName;

    public PlainParameterKeyUpHandler(final ParameterControllerPanel controller, final String parameterName)
    {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onKeyUp(final KeyUpEvent event)
    {
      final SuggestBox textBox = (SuggestBox) event.getSource();
      final String text = textBox.getText();
      String value = labelToValueMap.get(text);
      if (value == null)
      {
        value = text;
      }
      if (ReportViewerUtil.isEmpty(value))
      {
        controller.getParameterMap().setSelectedValue(parameterName, null);
      }
      else
      {
        controller.getParameterMap().setSelectedValue(parameterName, value);
      }
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
      {
        // on enter, force update
        controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
      }
    }

  }

  private class PlainParameterSelectionHandler implements SelectionHandler<Suggestion>
  {
    private ParameterControllerPanel controller;
    private String parameterName;

    public PlainParameterSelectionHandler(final ParameterControllerPanel controller, final String parameterName)
    {
      this.controller = controller;
      this.parameterName = parameterName;
    }

    public void onSelection(final SelectionEvent<Suggestion> event)
    {
      final String text = event.getSelectedItem().getReplacementString();
      String value = labelToValueMap.get(text);
      if (value == null)
      {
        value = text;
      }
      if (ReportViewerUtil.isEmpty(value))
      {
        controller.getParameterMap().setSelectedValue(parameterName, null);
      }
      else
      {
        controller.getParameterMap().setSelectedValue(parameterName, value);
      }
    }

  }

  public PlainParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    final List<ParameterSelection> selections = parameterElement.getSelections();
    for (int i = 0; i < selections.size(); i++)
    {
      final ParameterSelection choiceElement = selections.get(i);
      final String choiceValue = choiceElement.getValue(); //$NON-NLS-1$
      final String choiceLabel = choiceElement.getLabel(); //$NON-NLS-1$
      if (choiceLabel != null && choiceLabel.length() > 0)
      {
        oracle.add(choiceLabel);
        labelToValueMap.put(choiceLabel, choiceValue);
      }
    }

    final SuggestBox textBox = new SuggestBox(oracle);
    if (selections.isEmpty())
    {
      textBox.setText(""); //$NON-NLS-1$
    }
    else
    {
      final ParameterSelection parameterSelection = selections.get(0);
      final String labelText = parameterSelection.getLabel();
      if (labelText != null && labelText.length() > 0)
      {
        textBox.setText(labelText);
      }
      else
      {
        textBox.setValue(parameterSelection.getValue());
      }
    }
    textBox.addSelectionHandler(new PlainParameterSelectionHandler(controller, parameterElement.getName()));
    textBox.addKeyUpHandler(new PlainParameterKeyUpHandler(controller, parameterElement.getName()));
    setWidget(textBox);
  }

}
