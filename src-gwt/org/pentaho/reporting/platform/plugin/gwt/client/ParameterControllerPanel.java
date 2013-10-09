/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;
import org.pentaho.reporting.platform.plugin.gwt.client.images.DisclosureImages;
import org.pentaho.reporting.platform.plugin.gwt.client.images.PageImages;

public class ParameterControllerPanel extends VerticalPanel {
  public enum ParameterSubmitMode {
    USERINPUT, MANUAL, INITIAL
  }

  private class ParameterRequestCallback implements RequestCallback {
    private ParameterSubmitMode submitMode;

    public void setSubmitMode( final ParameterSubmitMode submitMode ) {
      this.submitMode = submitMode;
    }

    public void onError( final Request request, final Throwable exception ) {
      setEnabled( true );
      ReportViewerUtil.showErrorDialog( messages, messages.getString( "couldNotFetchParams" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void onResponseReceived( final Request request, final Response response ) {
      final int code = response.getStatusCode();
      if ( code != Response.SC_OK ) {
        ReportViewerUtil.showErrorDialog( messages, messages.getString( "couldNotFetchParams" ) ); //$NON-NLS-1$ //$NON-NLS-2$
        return;
      }

      final Document resultDoc;
      try {
        resultDoc = XMLParser.parse( response.getText() );
      } catch ( Exception e ) {
        ReportViewerUtil.showErrorDialog( messages, response.getText() ); //$NON-NLS-1$
        return;
      }

      final Element parametersElement = resultDoc.getDocumentElement();

      final HashMap<String, ArrayList<String>> errors = buildErrors( resultDoc ); //$NON-NLS-1$
      final ArrayList<String> globalErrors = errors.get( null );

      parameterDefinition = parseParameterDefinition( parametersElement );
      if ( submitMode == ParameterSubmitMode.INITIAL ) {
        submitParametersOnChangeCheckBox.setValue( parameterDefinition.isAllowAutosubmit(), false );
        if ( parameterDefinition.isAllowAutosubmit() ) {
          // fire the parameter submit a second time, this time with the pagination information ..
          fetchParameters( ParameterSubmitMode.MANUAL );
          return;
        }
      } else {
        parameterDefinition.setAutoSubmitUI( submitParametersOnChangeCheckBox.getValue() == Boolean.TRUE );
      }

      parameterWidgets.clear();
      clear();
      setEnabled( true );

      if ( parameterDefinition.isShowParameterUi() ) {
        buildParameterPanel( submitMode, errors, globalErrors, parameterDefinition );
        forceFocus();
      } else {
        if ( globalErrors != null && globalErrors.isEmpty() == false ) {
          add( buildGlobalErrors( globalErrors ) );
        }

        // create a new parameter value map
        // Only add parameter if there is at least one selection.
        // Without a selection, a parameter is <null>.
        parameterMap = new ParameterValues();
        final ParameterGroup[] parameterGroups = parameterDefinition.getParameterGroups();
        for ( final ParameterGroup group : parameterGroups ) {
          for ( final Parameter parameterElement : group.getParameters() ) {
            final String parameterName = parameterElement.getName(); //$NON-NLS-1$
            final List<ParameterSelection> list = parameterElement.getSelections();
            if ( list.isEmpty() == false ) {
              final ArrayList<String> parameterSelections = new ArrayList<String>();
              for ( final ParameterSelection selection : list ) {
                if ( selection.isSelected() ) {
                  parameterSelections.add( selection.getValue() );
                }
              }
              parameterMap.setSelectedValues( parameterName, parameterSelections
                  .toArray( new String[parameterSelections.size()] ) );
            } else {
              parameterMap.setSelectedValue( parameterName, null );
            }

          }
        }

        final boolean isPromptNeeded = parameterDefinition.isPromptNeeded();
        final boolean paginate = parameterDefinition.isPaginate();
        // do not show the parameter UI, but we must still fire events
        // if prompt is not needed
        if ( isPromptNeeded == false ) {
          if ( paginate ) { //$NON-NLS-1$ //$NON-NLS-2$
            add( buildPaginationController( parameterDefinition.getProcessingState() ) );
          }
          if ( parameterDefinition.isAllowAutosubmit() || submitMode == ParameterSubmitMode.MANUAL ) {
            showReport();
          } else {
            showBlankPage();
          }
        } else {
          showBlankPage();
        }

      }

      container.init();
    }

  }

  private class SubmitParameterListener implements ClickHandler {

    private SubmitParameterListener() {
    }

    public void onClick( final ClickEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( promptNeeded() == false ) {
        // async call
        fetchParameters( ParameterSubmitMode.MANUAL );
      }
    }
  }

  private class GotoFirstPageClickHandler implements ClickHandler {
    private int finalAcceptedPage;

    private GotoFirstPageClickHandler( final int finalAcceptedPage ) {
      this.finalAcceptedPage = finalAcceptedPage;
    }

    public void onClick( final ClickEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( finalAcceptedPage > 0 ) {
        parameterMap.setSelectedValue( "accepted-page", "0" ); //$NON-NLS-1$
        fetchParameters( ParameterSubmitMode.MANUAL );
      }
    }
  }

  private class GotoLastPageClickHandler implements ClickHandler {
    private int finalAcceptedPage;
    private int finalPageCount;

    private GotoLastPageClickHandler( final int finalAcceptedPage, final int finalPageCount ) {
      this.finalAcceptedPage = finalAcceptedPage;
      this.finalPageCount = finalPageCount;
    }

    public void onClick( final ClickEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( finalAcceptedPage + 1 < finalPageCount ) {
        parameterMap.setSelectedValue( "accepted-page", String.valueOf( finalPageCount - 1 ) ); //$NON-NLS-1$
        fetchParameters( ParameterSubmitMode.MANUAL );
      }
    }
  }

  private class GotoPrevPageClickHandler implements ClickHandler {
    private int finalAcceptedPage;

    private GotoPrevPageClickHandler( final int finalAcceptedPage ) {
      this.finalAcceptedPage = finalAcceptedPage;
    }

    public void onClick( final ClickEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( finalAcceptedPage > 0 ) {
        parameterMap.setSelectedValue( "accepted-page", String.valueOf( finalAcceptedPage - 1 ) ); //$NON-NLS-1$
        fetchParameters( ParameterSubmitMode.MANUAL );
      }
    }
  }

  private class GotoNextPageClickHandler implements ClickHandler {
    private int finalAcceptedPage;
    private int finalPageCount;

    private GotoNextPageClickHandler( final int finalAcceptedPage, final int finalPageCount ) {
      this.finalAcceptedPage = finalAcceptedPage;
      this.finalPageCount = finalPageCount;
    }

    public void onClick( final ClickEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( finalAcceptedPage + 1 < finalPageCount ) {
        parameterMap.setSelectedValue( "accepted-page", String.valueOf( finalAcceptedPage + 1 ) ); //$NON-NLS-1$
        fetchParameters( ParameterSubmitMode.MANUAL );
      }
    }
  }

  private class PageInputHandler implements KeyUpHandler {
    private final TextBox pageBox;
    private final int finalPageCount;

    public PageInputHandler( final TextBox pageBox, final int finalPageCount ) {
      this.pageBox = pageBox;
      this.finalPageCount = finalPageCount;
    }

    public void onKeyUp( final KeyUpEvent event ) {
      if ( enabled == false ) {
        return;
      }

      if ( event.getNativeKeyCode() != KeyCodes.KEY_ENTER ) {
        return;
      }

      String error = null;
      try {
        final int page = Integer.parseInt( pageBox.getText() );
        if ( page < 1 ) {
          throw new Exception( messages.getString(
              "firstPageMustBePositive", "<BR>First page must a positive number<BR><BR>" ) ); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ( page > finalPageCount ) {
          throw new Exception( messages.getString(
              "pageOutOfRange", "<BR>Page out of range, max page is : {0} <BR><BR>", "" + finalPageCount ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        parameterMap.setSelectedValue( "accepted-page", String.valueOf( page - 1 ) ); //$NON-NLS-1$
        fetchParameters( ParameterSubmitMode.MANUAL );
      } catch ( NumberFormatException t ) {
        error = messages.getString( "numericDigitsOnly", "<BR>Page number must contain numeric digits only.<BR><BR>" ); //$NON-NLS-1$ //$NON-NLS-2$
      } catch ( Throwable t ) {
        error = t.getMessage();
      }

      if ( error != null ) {
        ReportViewerUtil.showErrorDialog( messages, error );
      }
    }
  }

  private class AutoSubmitChangeHandler implements ValueChangeHandler<Boolean> {
    private AutoSubmitChangeHandler() {
    }

    public void onValueChange( final ValueChangeEvent<Boolean> booleanValueChangeEvent ) {
      if ( parameterDefinition != null ) {
        parameterDefinition.setAutoSubmitUI( booleanValueChangeEvent.getValue() == Boolean.TRUE );
      }
    }
  }

  private class GrabFocusHandler implements OpenHandler<DisclosurePanel> {
    public void onOpen( final OpenEvent<DisclosurePanel> disclosurePanelOpenEvent ) {
      forceFocus();
    }
  }

  private ParameterRequestCallback parameterRequestCallback = new ParameterRequestCallback();
  private ReportContainer container;

  // all the parameters will be forced into strings
  private ParameterValues parameterMap;

  private DisclosurePanel parameterDisclosurePanel;
  private VerticalPanel parameterContainer;
  private CheckBox submitParametersOnChangeCheckBox;
  private final ResourceBundle messages;

  private Button submitSubscriptionButton;
  private Button submitParametersButton;
  private ArrayList<ParameterUI> parameterWidgets;
  private ParameterDefinition parameterDefinition;
  private TextBox pageBox;
  private boolean enabled;

  public ParameterControllerPanel( final ReportContainer container, final ResourceBundle messages ) {
    this.messages = messages;
    this.container = container;
    this.enabled = true;

    parameterMap = new ParameterValues();
    parameterWidgets = new ArrayList<ParameterUI>();

    parameterDisclosurePanel =
        new DisclosurePanel( DisclosureImages.images,
            messages.getString( "reportParameters", "Report Parameters" ), true ); //$NON-NLS-1$ //$NON-NLS-2$
    parameterDisclosurePanel.setStyleName( "pentaho-disclosure-panel" ); //$NON-NLS-1$
    parameterDisclosurePanel.setOpen( true );
    parameterDisclosurePanel.setWidth( "100%" ); //$NON-NLS-1$

    // fix for BISERVER-6027 - on open of the panel, set the focus to the focusWidget
    parameterDisclosurePanel.addOpenHandler( new GrabFocusHandler() );

    submitParametersButton = new Button( messages.getString( "viewReport", "View Report" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    submitParametersButton.setStyleName( "pentaho-button" );
    submitParametersButton.addClickHandler( new SubmitParameterListener() );

    submitSubscriptionButton = new Button( messages.getString( "schedule", "Schedule" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    submitSubscriptionButton.setStyleName( "pentaho-button" );

    submitParametersOnChangeCheckBox = new CheckBox( messages.getString( "autoSubmit", "Auto-Submit" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    submitParametersOnChangeCheckBox.setTitle( messages.getString( "submitTooltip" ) ); //$NON-NLS-1$
    submitParametersOnChangeCheckBox.addValueChangeHandler( new AutoSubmitChangeHandler() );

    setWidth( "100%" ); //$NON-NLS-1$
    setStyleName( "parameter-application" ); //$NON-NLS-1$

    parameterContainer = new VerticalPanel();
    parameterContainer.setStyleName( "parameter-container" ); //$NON-NLS-1$
    parameterContainer.setWidth( "100%" ); //$NON-NLS-1$

    // async call
    fetchParameters( ParameterSubmitMode.INITIAL );
  }

  public void setEnabled( final boolean enabled ) {
    this.enabled = enabled;

    WaitPopup.getInstance().setVisible( enabled == false );

    submitParametersButton.setEnabled( enabled );
    submitParametersOnChangeCheckBox.setEnabled( enabled );
    submitSubscriptionButton.setEnabled( enabled );
    if ( pageBox != null ) {
      pageBox.setEnabled( enabled );
    }

    for ( int i = 0; i < parameterWidgets.size(); i++ ) {
      final ParameterUI parameterUI = parameterWidgets.get( i );
      parameterUI.setEnabled( enabled );
    }

  }

  private ParameterDefinition parseParameterDefinition( final Element element ) {
    final ParameterDefinition parameterDefinition = new ParameterDefinition();
    parameterDefinition.setPromptNeeded( "true".equals( element.getAttribute( "is-prompt-needed" ) ) ); // NON-NLS
    parameterDefinition
       .setIgnoreBiServer5538( "true".equals( element.getAttribute( "ignore-biserver-5538" ) ) ); // NON-NLS
    parameterDefinition.setPaginate( "true".equals( element.getAttribute( "paginate" ) ) ); // NON-NLS
    parameterDefinition.setSubscribe( "true".equals( element.getAttribute( "subscribe" ) ) ); // NON-NLS
    parameterDefinition.setLayout( element.getAttribute( "layout" ) ); // NON-NLS

    final ProcessingState state = new ProcessingState();
    state.setPage( ReportViewerUtil.parseInt( element.getAttribute( "accepted-page" ), 0 ) ); // NON-NLS
    state.setTotalPages( ReportViewerUtil.parseInt( element.getAttribute( "page-count" ), 0 ) ); // NON-NLS
    parameterDefinition.setProcessingState( state );

    final String autoSubmit = element.getAttribute( "autoSubmit" );
    if ( "true".equals( autoSubmit ) ) {
      parameterDefinition.setAutoSubmit( Boolean.TRUE );
    } else if ( "false".equals( autoSubmit ) ) {
      parameterDefinition.setAutoSubmit( Boolean.FALSE );
    } else {
      parameterDefinition.setAutoSubmit( null );
    }

    parameterDefinition.setAutoSubmitUI( "true".equals( element.getAttribute( "autoSubmitUI" ) ) ); // NON-NLS

    final NodeList parameterNodes = element.getElementsByTagName( "parameter" ); // NON-NLS
    if ( parameterNodes != null ) {
      for ( int i = 0; i < parameterNodes.getLength(); i++ ) {
        final Element parameterElement = (Element) parameterNodes.item( i );
        final String name = parameterElement.getAttribute( "name" ); // NON-NLS
        final Parameter parameter = new Parameter( name );
        parameter.setMandatory( "true".equals( parameterElement.getAttribute( "is-mandatory" ) ) ); // NON-NLS
        parameter.setStrict( "true".equals( parameterElement.getAttribute( "is-strict" ) ) ); // NON-NLS
        parameter.setList( "true".equals( parameterElement.getAttribute( "is-list" ) ) ); // NON-NLS
        parameter.setMultiSelect( "true".equals( parameterElement.getAttribute( "is-multi-select" ) ) ); // NON-NLS
        parameter.setType( parameterElement.getAttribute( "type" ) ); // NON-NLS
        parameter.setTimezoneHint( parameterElement.getAttribute( "timezone-hint" ) ); // NON-NLS

        final NodeList attributes = parameterElement.getElementsByTagName( "attribute" ); // NON-NLS
        final int length = attributes.getLength();
        for ( int aidx = 0; aidx < length; aidx++ ) {
          final Element item = (Element) attributes.item( aidx );
          final String namespace = item.getAttribute( "namespace" );
          final String attrName = item.getAttribute( "name" );
          final String attrValue = item.getAttribute( "value" );
          if ( ReportViewerUtil.isEmpty( attrName ) || ReportViewerUtil.isEmpty( attrValue ) ) {
            continue;
          }
          if ( ReportViewerUtil.isEmpty( namespace ) ) {
            parameter.setAttribute( Parameter.CORE_NAMESPACE, attrName, attrValue );
          } else {
            parameter.setAttribute( namespace, attrName, attrValue );
          }
        }

        final NodeList list = parameterElement.getElementsByTagName( "value" );
        for ( int videx = 0; videx < list.getLength(); videx++ ) {
          final Element valueElement = (Element) list.item( videx );
          final String label = valueElement.getAttribute( "label" ); // NON-NLS
          String value;
          if ( "true".equals( valueElement.getAttribute( "null" ) ) ) {
            value = null;
          } else {
            value = valueElement.getAttribute( "value" ); // NON-NLS
            if ( value == null ) {
              // XML parser treats empty strings like undefined attributes.
              value = "";
            }
          }
          String type = valueElement.getAttribute( "type" ); // NON-NLS
          if ( ReportViewerUtil.isEmpty( type ) ) {
            type = parameter.getType();
          }
          final boolean selected = "true".equals( valueElement.getAttribute( "selected" ) ); // NON-NLS

          final String normValue = ReportViewerUtil.normalizeParameterValue( parameter, type, value );
          parameter.addSelection( new ParameterSelection( type, normValue, selected, label ) );
        }

        String parameterGroupName = parameter.getAttribute( "parameter-group" ); //$NON-NLS-1$
        if ( ReportViewerUtil.isEmpty( parameterGroupName ) ) {
          // default group
          parameterGroupName = "parameters"; //$NON-NLS-1$
        }
        ParameterGroup parameterGroup = parameterDefinition.getParameterGroup( parameterGroupName );
        if ( parameterGroup == null ) {
          final String parameterGroupLabel = parameter.getAttribute( "parameter-group-label" ); //$NON-NLS-1$
          parameterGroup = new ParameterGroup( parameterGroupName, parameterGroupLabel );
          parameterDefinition.addParameterGroup( parameterGroup );
        }
        parameterGroup.addParameter( parameter );
      }
    }
    return parameterDefinition;
  }

  private void buildParameterPanel( final ParameterSubmitMode mode, final HashMap<String, ArrayList<String>> errors,
      final ArrayList<String> globalErrors, final ParameterDefinition parametersElement ) {
    try {
      if ( globalErrors != null && globalErrors.isEmpty() == false ) {
        add( buildGlobalErrors( globalErrors ) );
      }

      // build parameter UI from document
      parameterContainer.clear();
      parameterWidgets.clear();

      // create a new parameter value map
      parameterMap = new ParameterValues();

      final String layout = parametersElement.getLayout();
      int totalParameterAdded = 0;
      // must preserve order
      final ParameterGroup[] parameterGroups = parametersElement.getParameterGroups();
      for ( int i = 0; i < parameterGroups.length; i++ ) {
        final ParameterGroup group = parameterGroups[i];

        final Panel parameterGroupPanel;
        if ( layout.equals( "flow" ) ) { //$NON-NLS-1$
          parameterGroupPanel = new FlowPanel();
        } else if ( layout.equals( "horizontal" ) ) {
          parameterGroupPanel = new HorizontalPanel();
        } else {
          parameterGroupPanel = new VerticalPanel();
        }
        final String groupLabel = group.getLabel(); //$NON-NLS-1$

        int parametersAdded = 0;

        for ( final Parameter parameterElement : group.getParameters() ) {
          final String parameterName = parameterElement.getName(); //$NON-NLS-1$
          final List<ParameterSelection> list = parameterElement.getSelections();
          if ( list.isEmpty() == false ) {
            final ArrayList<String> parameterSelections = new ArrayList<String>();
            for ( final ParameterSelection selection : list ) {
              if ( selection.isSelected() ) {
                parameterSelections.add( selection.getValue() );
              }
            }
            parameterMap.setSelectedValues( parameterName, parameterSelections.toArray( new String[parameterSelections
                .size()] ) );
          } else {
            parameterMap.setSelectedValue( parameterName, null );
          }

          if ( parameterElement.isHidden() ) {
            continue;
          }

          final String label = parameterElement.getLabel(); //$NON-NLS-1$
          final String tooltip = parameterElement.getTooltip(); //$NON-NLS-1$
          final Label parameterLabel = new Label( label );
          parameterLabel.setTitle( tooltip );
          parameterLabel.setStyleName( "parameter-label" ); //$NON-NLS-1$

          final VerticalPanel parameterPanel = new VerticalPanel();
          parameterPanel.setStyleName( "parameter" ); //$NON-NLS-1$
          parameterPanel.setTitle( tooltip );
          parameterPanel.add( parameterLabel );

          final Widget parameterWidget = buildParameterWidget( parametersElement, parameterElement );
          if ( parameterWidget == null ) {
            continue;
          }

          parametersAdded += 1;

          // only add the parameter if it has a UI
          final ArrayList<String> parameterErrors = errors.get( parameterName );
          if ( parameterErrors != null ) {
            for ( final String error : parameterErrors ) {
              final Label errorLabel = new Label( error );
              errorLabel.setStyleName( "parameter-error-label" ); // NON-NLS
              parameterPanel.add( errorLabel );
            }
            parameterPanel.setStyleName( "parameter-error" ); //$NON-NLS-1$
          }

          if ( parameterWidget instanceof ParameterUI ) {
            parameterWidgets.add( (ParameterUI) parameterWidget );
          }
          parameterPanel.add( parameterWidget );

          if ( layout.equals( "flow" ) ) { //$NON-NLS-1$
            final SimplePanel div = new SimplePanel();
            div.setStyleName( "parameter-flow" ); //$NON-NLS-1$
            div.add( parameterPanel );
            parameterGroupPanel.add( div );
          } else {
            parameterGroupPanel.add( parameterPanel );
          }
        }

        totalParameterAdded += parametersAdded;
        if ( parametersAdded > 0 ) {
          if ( parametersElement.isSubscribe() ) {
            final CaptionPanel parameterGroupCaptionPanel = new CaptionPanel();
            if ( groupLabel != null && !groupLabel.equals( "" ) ) {
              parameterGroupCaptionPanel.setCaptionText( groupLabel );
            }
            parameterGroupCaptionPanel.setStyleName( "parameter" ); //$NON-NLS-1$
            parameterGroupCaptionPanel.setContentWidget( parameterGroupPanel );

            final Panel panel = new SimplePanel();
            panel.setStyleName( "parameter-wrapper" );
            panel.add( parameterGroupCaptionPanel );
            parameterContainer.add( panel );
          } else {
            final Panel panel = new SimplePanel();
            panel.setStyleName( "parameter-wrapper" );
            panel.add( parameterGroupPanel );
            parameterContainer.add( panel );
          }
        }
      }

      if ( totalParameterAdded > 0 ) {
        // add parameter submit button/auto-submit checkbox
        final FlowPanel submitPanel = new FlowPanel();
        submitPanel.setWidth( "100%" ); //$NON-NLS-1$
        submitPanel.setStyleName( "parameter-submit-panel" ); //$NON-NLS-1$
        if ( parametersElement.isSubscribe() ) { //$NON-NLS-1$ //$NON-NLS-2$
          submitPanel.add( submitSubscriptionButton );
        }
        submitPanel.add( submitParametersButton );

        // handle the auto-submit defaults.
        final Boolean autoSubmitAttr = parametersElement.getAutoSubmit();
        if ( Boolean.TRUE.equals( autoSubmitAttr ) ) {
          submitParametersOnChangeCheckBox.setValue( true );
        } else if ( Boolean.FALSE.equals( autoSubmitAttr ) ) {
          submitParametersOnChangeCheckBox.setValue( false );
        } else {
          // BISERVER-3821 Provide ability to remove Auto-Submit check box from report viewer
          // only show the UI for the autosubmit checkbox if no preference exists
          submitPanel.add( submitParametersOnChangeCheckBox );
          submitParametersOnChangeCheckBox.setValue( parametersElement.isAutoSubmitUI() );
        }

        parameterContainer.add( submitPanel );

        final SimplePanel parameterPanelWrapper = new SimplePanel();
        parameterPanelWrapper.setWidget( parameterContainer );
        parameterPanelWrapper.setStyleName( "parameter-content-panel" );
        parameterPanelWrapper.getElement().addClassName( "pentaho-rounded-panel-bottom-lr" );
        parameterPanelWrapper.getElement().addClassName( "pentaho-shadow" );

        parameterDisclosurePanel.setContent( parameterPanelWrapper );
        add( parameterDisclosurePanel );
      }

      // add pagination controller (if needed)
      if ( parametersElement.isPaginate() ) { //$NON-NLS-1$ //$NON-NLS-2$
        add( buildPaginationController( parametersElement.getProcessingState() ) );
      }
      if ( globalErrors != null && globalErrors.isEmpty() == false ) {
        add( buildGlobalErrors( globalErrors ) );
      }

      // do not show the parameter UI, but we must still fire events
      // if prompt is not needed
      if ( parametersElement.isPromptNeeded() == false ) {
        final boolean flag = ( mode == ParameterSubmitMode.MANUAL );
        if ( parametersElement.isAllowAutosubmit() || flag ) {
          showReport();
        } else {
          showBlankPage();
        }
      } else {
        showBlankPage();
      }
    } catch ( Exception e ) {
      Window.alert( "Failed in Build parameter panel" );
    }
  }

  private HashMap<String, ArrayList<String>> buildErrors( final Document doc ) {
    final HashMap<String, ArrayList<String>> errorMap = new HashMap<String, ArrayList<String>>();
    final NodeList errors = doc.getElementsByTagName( "error" );
    for ( int i = 0; i < errors.getLength(); i++ ) {
      final Element error = (Element) errors.item( i );
      final String parameter = error.getAttribute( "parameter" ); //$NON-NLS-1$
      final String msg = error.getAttribute( "message" ); //$NON-NLS-1$
      ArrayList<String> errorList = errorMap.get( parameter );
      if ( errorList == null ) {
        errorList = new ArrayList<String>();
        errorMap.put( parameter, errorList );
      }
      errorList.add( msg );
    }

    final NodeList globalErrors = doc.getElementsByTagName( "global-error" ); //$NON-NLS-1$
    for ( int i = 0; i < globalErrors.getLength(); i++ ) {
      final Element error = (Element) globalErrors.item( i );
      final String msg = error.getAttribute( "message" ); //$NON-NLS-1$
      ArrayList<String> errorList = errorMap.get( null );
      if ( errorList == null ) {
        errorList = new ArrayList<String>();
        errorMap.put( null, errorList );
      }
      errorList.add( msg );
    }
    return errorMap;
  }

  private Widget buildGlobalErrors( final ArrayList<String> errors ) {
    final VerticalPanel parameterPanel = new VerticalPanel();
    parameterPanel.setStyleName( "parameter-error" ); //$NON-NLS-1$

    // only add the parameter if it has a UI
    if ( errors != null ) {
      for ( final String error : errors ) {
        final Label errorLabel = new Label( error );
        errorLabel.setStyleName( "parameter-error-label" ); //$NON-NLS-1$
        parameterPanel.add( errorLabel );
      }
    }
    return parameterPanel;
  }

  private Widget buildPaginationController( final ProcessingState parametersElement ) {
    // need to add/build UI for pagination controls
    final int finalPageCount = parametersElement.getTotalPages();
    final int finalAcceptedPage;
    if ( parametersElement.getPage() >= finalPageCount ) {
      // we can't accept pages out of range, this can happen if we are on a page and then change a parameter value
      // resulting in a new report with less pages
      // when this happens, we'll just reduce the accepted page
      finalAcceptedPage = Math.max( 0, finalPageCount - 1 );
    } else {
      finalAcceptedPage = Math.max( 0, parametersElement.getPage() );
    }
    // add our default page, so we can keep this between selections of other parameters, otherwise it will not be on the
    // set of params are default back to zero (page 1)
    parameterMap.setSelectedValue( "accepted-page", String.valueOf( finalAcceptedPage ) ); //$NON-NLS-1$

    final Image backToFirstPage = PageImages.images.backToFirstPage().createImage();
    backToFirstPage.setStyleName( "pageControllerButton" ); //$NON-NLS-1$
    if ( finalAcceptedPage <= 0 ) {
      PageImages.images.backToFirstPageDisabled().applyTo( backToFirstPage );
    } else {
      backToFirstPage.addClickHandler( new GotoFirstPageClickHandler( finalAcceptedPage ) );
      backToFirstPage.addMouseOverHandler( new MouseOverHandler() {
        public void onMouseOver( final MouseOverEvent event ) {
          PageImages.images.backToFirstPageHover().applyTo( backToFirstPage );
        }
      } );
      backToFirstPage.addMouseOutHandler( new MouseOutHandler() {
        public void onMouseOut( final MouseOutEvent event ) {
          PageImages.images.backToFirstPage().applyTo( backToFirstPage );
        }
      } );
    }

    final Image backPage = PageImages.images.backButton().createImage();
    backPage.setStyleName( "pageControllerButton" ); //$NON-NLS-1$
    if ( finalAcceptedPage <= 0 ) {
      PageImages.images.backButtonDisabled().applyTo( backPage );
    } else {
      backPage.addClickHandler( new GotoPrevPageClickHandler( finalAcceptedPage ) );
      backPage.addMouseOverHandler( new MouseOverHandler() {
        public void onMouseOver( final MouseOverEvent event ) {
          PageImages.images.backButtonHover().applyTo( backPage );
        }
      } );
      backPage.addMouseOutHandler( new MouseOutHandler() {
        public void onMouseOut( final MouseOutEvent event ) {
          PageImages.images.backButton().applyTo( backPage );
        }
      } );
    }

    final Image forwardPage = PageImages.images.forwardButton().createImage();
    forwardPage.setStyleName( "pageControllerButton" ); //$NON-NLS-1$
    if ( finalAcceptedPage + 1 >= finalPageCount ) {
      PageImages.images.forwardButtonDisabled().applyTo( forwardPage );
    } else {
      forwardPage.addClickHandler( new GotoNextPageClickHandler( finalAcceptedPage, finalPageCount ) );
      forwardPage.addMouseOverHandler( new MouseOverHandler() {
        public void onMouseOver( final MouseOverEvent event ) {
          PageImages.images.forwardButtonHover().applyTo( forwardPage );
        }
      } );
      forwardPage.addMouseOutHandler( new MouseOutHandler() {
        public void onMouseOut( final MouseOutEvent event ) {
          PageImages.images.forwardButton().applyTo( forwardPage );
        }
      } );
    }

    final Image forwardToLastPage = PageImages.images.forwardToLastPage().createImage();
    forwardToLastPage.setStyleName( "pageControllerButton" ); //$NON-NLS-1$
    if ( finalAcceptedPage + 1 >= finalPageCount ) {
      PageImages.images.forwardToLastPageDisabled().applyTo( forwardToLastPage );
    } else {
      forwardToLastPage.addClickHandler( new GotoLastPageClickHandler( finalAcceptedPage, finalPageCount ) );
      forwardToLastPage.addMouseOverHandler( new MouseOverHandler() {
        public void onMouseOver( final MouseOverEvent event ) {
          PageImages.images.forwardToLastPageHover().applyTo( forwardToLastPage );
        }
      } );
      forwardToLastPage.addMouseOutHandler( new MouseOutHandler() {
        public void onMouseOut( final MouseOutEvent event ) {
          PageImages.images.forwardToLastPage().applyTo( forwardToLastPage );
        }
      } );
    }

    pageBox = new TextBox();
    pageBox.setTextAlignment( TextBox.ALIGN_RIGHT );
    pageBox.addKeyUpHandler( new PageInputHandler( pageBox, finalPageCount ) );
    // pages are zero based, but expose them to the user as 1 based
    if ( finalAcceptedPage <= 0 ) {
      pageBox.setText( "1" ); //$NON-NLS-1$
    } else {
      pageBox.setText( String.valueOf( finalAcceptedPage + 1 ) ); //$NON-NLS-1$
    }
    pageBox.setVisibleLength( 3 );

    final HorizontalPanel pageControlPanel = new HorizontalPanel();
    pageControlPanel.setSpacing( 1 );
    pageControlPanel.setVerticalAlignment( HasVerticalAlignment.ALIGN_MIDDLE );
    pageControlPanel.add( backToFirstPage );
    pageControlPanel.add( backPage );

    final String pageStr = "&nbsp;&nbsp;" + messages.getString( "page", "Page" ) + "&nbsp;"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    pageControlPanel.add( new HTML( pageStr ) );
    pageControlPanel.add( pageBox );
    final String pageCountStr = "&nbsp;" + messages.getString( "of", "of" ) + "&nbsp;" + finalPageCount + "&nbsp;"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    pageControlPanel.add( new HTML( pageCountStr ) );

    pageControlPanel.add( forwardPage );
    pageControlPanel.add( forwardToLastPage );

    final HorizontalPanel pageControlPanelWrapper = new HorizontalPanel();
    pageControlPanelWrapper.setStyleName( "pageControllerPanel" ); //$NON-NLS-1$
    pageControlPanelWrapper.setWidth( "100%" ); //$NON-NLS-1$
    pageControlPanelWrapper.add( pageControlPanel );

    return pageControlPanelWrapper;
  }

  private Widget buildParameterWidget( final ParameterDefinition parameterDefinition,
                                       final Parameter parameterElement ) {
    String renderType = parameterElement.getAttribute( "parameter-render-type" ); //$NON-NLS-1$
    if ( renderType != null ) {
      renderType = renderType.trim();
    }

    final boolean isStrict = parameterElement.isStrict(); //$NON-NLS-1$ //$NON-NLS-2$
    if ( isStrict && parameterElement.hasValues() == false ) {
      // if the parameter is strict but we have no valid choices for it, it is impossible
      // for the user to give it a value, so we will hide this parameter
      // it is highly likely that the parameter is driven by another parameter which
      // doesn't have a value yet, so eventually, we'll show this parameter.. we hope
      return null;
    }

    if ( "radio".equals( renderType ) || "checkbox".equals( renderType ) ) //$NON-NLS-1$ //$NON-NLS-2$
    {
      return new CheckBoxParameterUI( this, parameterElement );
    } else if ( "togglebutton".equals( renderType ) ) { //$NON-NLS-1$
      return new ToggleButtonParameterUI( this, parameterElement );
    } else if ( "list".equals( renderType ) ) {
      return new ListParameterUI( this, parameterElement );
    } else if ( "dropdown".equals( renderType ) ) { //$NON-NLS-1$ //$NON-NLS-2$
      return new DropDownParameterUI( this, parameterDefinition, parameterElement );
    } else if ( "datepicker".equals( renderType ) ) { //$NON-NLS-1$
      return new DateParameterUI( this, parameterElement );
    } else if ( "multi-line".equals( renderType ) ) { //$NON-NLS-1$
      return new TextAreaParameterUI( this, parameterElement );
    } else {
      return new PlainParameterUI( this, parameterElement );
    }
  }

  public void fetchParameters( final ParameterSubmitMode submitMode ) {
    boolean paginate = false;
    container.showBlank();

    if ( submitMode == ParameterSubmitMode.INITIAL ) {
      paginate = false;
    } else if ( submitMode == ParameterSubmitMode.USERINPUT ) {
      if ( parameterDefinition == null || parameterDefinition.isAllowAutosubmit() == false ) {
        // only parameter without pagination of content ..
        paginate = false;
      } else {
        // user enabled auto-submit, so lets give him the full package..
        paginate = true;
      }
    } else {
      // manual requests always get the full treatment ..
      paginate = true;
    }
    final RequestBuilder requestBuilder =
        new RequestBuilder( RequestBuilder.GET, ReportViewerUtil.buildParameterUrl( paginate, parameterMap,
            parameterDefinition ) );
    parameterRequestCallback.setSubmitMode( submitMode );
    requestBuilder.setCallback( parameterRequestCallback );
    try {
      setEnabled( false );
      requestBuilder.send();
    } catch ( RequestException re ) {
      Window.alert( messages.getString( "couldNotFetchParameters", "Could not fetch parameter metadata from server." ) ); //$NON-NLS-1$ //$NON-NLS-2$
      setEnabled( true );
    }
  }

  private boolean promptNeeded() {
    // before we submit, let's check prompting needs
    boolean promptNeeded = false;
    String message = "<BR>"; //$NON-NLS-1$
    for ( final Parameter parameter : parameterDefinition.getParameter() ) {
      if ( parameter.isMandatory() ) { //$NON-NLS-1$ //$NON-NLS-2$
        // then let's make sure we have a value for it
        final String[] paramList = parameterMap.getParameterValues( parameter.getName() ); //$NON-NLS-1$
        if ( paramList == null || paramList.length == 0 ) {
          promptNeeded = true;
          final String paramTitle = parameter.getLabel(); //$NON-NLS-1$
          message += messages.getString( "parameterMissing", "Parameter [{0}] is missing.", paramTitle ); //$NON-NLS-1$ //$NON-NLS-2$
          message += "<BR>"; //$NON-NLS-1$
        }
      }
    }
    message += "<BR>"; //$NON-NLS-1$

    if ( promptNeeded ) {
      ReportViewerUtil.showMessageDialog( messages,
          messages.getString( "missingParameter", "Missing Parameter" ), message ); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return promptNeeded;
  }

  private void showReport() {
    container.setUrl( ReportViewerUtil.buildReportUrl( RENDER_TYPE.REPORT, parameterMap, parameterDefinition ) );
  }

  private void showBlankPage() {
    container.showBlank();
  }

  public ParameterValues getParameterMap() {
    return parameterMap;
  }

  // fix for BISERVER-6027 & BISERVER-4512 - set the focus into a textbox element to allow IE mouse access in these
  // elements
  private void forceFocus() {
    try {
      final com.google.gwt.dom.client.NodeList<com.google.gwt.dom.client.Element> inputElements =
          getElement().getElementsByTagName( "input" );
      if ( inputElements != null && inputElements.getLength() > 0 ) {
        for ( int i = 0; i < inputElements.getLength(); i++ ) {
          final com.google.gwt.dom.client.Element elem = inputElements.getItem( i );
          if ( "text".equalsIgnoreCase( elem.getAttribute( "type" ) ) ) {
            if ( !"date".equalsIgnoreCase( elem.getAttribute( "paramType" ) ) ) {
              // only focus things which are not date boxes
              elem.focus();
              break;
            }
          }
        }
      }
    } catch ( Exception e ) {
      ReportViewerUtil.checkStyleIgnore();
    }
  }

}
