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
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

public class PlainParameterUI extends SimplePanel
{
  private final Map<String, String> labelToValueMap = new HashMap<String, String>();

  private class PlainParameterKeyUpHandler implements KeyUpHandler
  {
    private ParameterControllerPanel controller;
    private List<String> parameterSelections;

    public PlainParameterKeyUpHandler(ParameterControllerPanel controller, List<String> parameterSelections)
    {
      this.controller = controller;
      this.parameterSelections = parameterSelections;
    }

    public void onKeyUp(KeyUpEvent event)
    {
      SuggestBox textBox = (SuggestBox) event.getSource();
      parameterSelections.clear();
      String text = textBox.getText();
      String value = labelToValueMap.get(text);
      if (value == null)
      {
        value = text;
      }
      parameterSelections.add(value);
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
      {
        // on enter, force update
        controller.fetchParameters(false);
      }
    }

  }

  private class PlainParameterSelectionHandler implements SelectionHandler<Suggestion>
  {
    private List<String> parameterSelections;

    public PlainParameterSelectionHandler(final List<String> parameterSelections)
    {
      this.parameterSelections = parameterSelections;
    }

    public void onSelection(SelectionEvent<Suggestion> event)
    {
      // SuggestBox textBox = (SuggestBox) event.getSource();
      parameterSelections.clear();
      String text = event.getSelectedItem().getReplacementString();
      String value = labelToValueMap.get(text);
      if (value == null)
      {
        value = text;
      }
      parameterSelections.add(value);
    }

  }

  public PlainParameterUI(final ParameterControllerPanel controller, final List<String> parameterSelections, final Element parameterElement)
  {
    // unknown, or PlainParameter
    final Map<String, String> valueToLabelMap = new HashMap<String, String>();

    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    NodeList choices = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
    for (int i = 0; i < choices.getLength(); i++)
    {
      final Element choiceElement = (Element) choices.item(i);
      final String choiceLabel = choiceElement.getAttribute("label"); //$NON-NLS-1$
      final String choiceValue = choiceElement.getAttribute("value"); //$NON-NLS-1$
      oracle.add(choiceLabel);
      labelToValueMap.put(choiceLabel, choiceValue);
      valueToLabelMap.put(choiceValue, choiceLabel);
    }

    final SuggestBox textBox = new SuggestBox(oracle);
    textBox.setText(""); //$NON-NLS-1$
    for (String text : parameterSelections)
    {
      String labelText = valueToLabelMap.get(text);
      if (labelText == null)
      {
        labelText = text;
      }
      // we need to get the label for the value
      textBox.setText(textBox.getText() + labelText);
    }
    textBox.addSelectionHandler(new PlainParameterSelectionHandler(parameterSelections));
    textBox.addKeyUpHandler(new PlainParameterKeyUpHandler(controller, parameterSelections));
    setWidget(textBox);
  }

}
