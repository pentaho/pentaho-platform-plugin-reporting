package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.List;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;

public class TextAreaParameterUI extends SimplePanel implements ParameterUI
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
      controller.getParameterMap().setSelectedValue(parameterName, value);
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
      {
        // on enter, force update
        controller.fetchParameters(ParameterControllerPanel.ParameterSubmitMode.USERINPUT);
      }
    }

  }

  private TextArea textBox;

  public TextAreaParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    final List<ParameterSelection> selections = parameterElement.getSelections();
    textBox = new TextArea();
    if (selections.isEmpty())
    {
      textBox.setText(""); //$NON-NLS-1$
    }
    else
    {
      ParameterSelection parameterSelection = null;
      for (int i = 0; i < selections.size(); i++)
      {
        final ParameterSelection selection = selections.get(i);
        if (selection.isSelected())
        {
          parameterSelection = selection;
        }
      }

      if (parameterSelection != null)
      {
        final String labelText = parameterSelection.getLabel();
        textBox.setText(labelText);
      }
    }
    textBox.addKeyUpHandler(new PlainParameterKeyUpHandler(controller, parameterElement.getName()));
    setWidget(textBox);
  }

  public void setEnabled(final boolean enabled)
  {
    textBox.setEnabled(enabled);
  }
}
