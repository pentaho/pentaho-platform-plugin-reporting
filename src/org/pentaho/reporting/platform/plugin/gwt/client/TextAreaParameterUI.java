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
import com.google.gwt.user.client.ui.TextArea;

public class TextAreaParameterUI extends SimplePanel
{
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
      final TextArea textBox = (TextArea) event.getSource();
      final String value = textBox.getText();
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

  public TextAreaParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    final List<ParameterSelection> selections = parameterElement.getSelections();
    final TextArea textBox = new TextArea();
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
    textBox.addKeyUpHandler(new PlainParameterKeyUpHandler(controller, parameterElement.getName()));
    setWidget(textBox);
  }

}