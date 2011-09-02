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
      if (dataFormat != null)
      {
        try
        {
          textBox.setStyleName("");
          final String text = ReportViewerUtil.createTransportObject(parameterElement, dataFormat.parse(value));
          controller.getParameterMap().setSelectedValue(parameterName, text);
        }
        catch (Exception e)
        {
          textBox.setStyleName("text-parse-error");
          // ignore partial values ..
//          controller.getParameterMap().setSelectedValue(null, value);
        }
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

  private TextArea textBox;
  private TextFormat dataFormat;
  private Parameter parameterElement;

  public TextAreaParameterUI(final ParameterControllerPanel controller, final Parameter parameterElement)
  {
    this.parameterElement = parameterElement;
    final List<ParameterSelection> selections = parameterElement.getSelections();
    textBox = new TextArea();

    final String dataType = parameterElement.getType();
    if (parameterElement.isList())
    {
      // formatting and lists are mutually exclusive.
      dataFormat = null;
    }
    else
    {
      // ParameterAttributeNames.Core.DATA_FORMAT
      final String dataFormatText = parameterElement.getAttribute("data-format");
      dataFormat = ReportViewerUtil.createTextFormat(dataFormatText, dataType);
    }

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
        if (dataFormat != null)
        {
          final Object rawObject = ReportViewerUtil.createRawObject(labelText, parameterElement);
          if (rawObject != null)
          {
            textBox.setText(dataFormat.format(rawObject));
          }
          else
          {
            textBox.setText(labelText);
          }
        }
        else
        {
          textBox.setText(labelText);
        }
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
