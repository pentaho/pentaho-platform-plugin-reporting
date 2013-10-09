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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.pentaho.gwt.widgets.client.utils.i18n.IResourceBundleLoadCallback;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;

/**
 * Global container class that performs a controlled initialization of the viewer application. The true magic happens in
 * the ReportContainer class.
 * 
 */
public class ReportViewer implements EntryPoint, IResourceBundleLoadCallback {
  private ResourceBundle messages = new ResourceBundle();
  private ReportContainer container;

  private ValueChangeHandler<String> historyHandler = new ValueChangeHandler<String>() {
    public void onValueChange( final ValueChangeEvent<String> event ) {
      initUI();
    }
  };

  public enum RENDER_TYPE {
    REPORT, XML, PARAMETER, DOWNLOAD
  }

  public void onModuleLoad() {
    messages.loadBundle( "messages/", "reportingMessages", true, ReportViewer.this ); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void bundleLoaded( final String bundleName ) {

    // build report container
    // this widget has:
    // +report parameter panel
    // +page controller (if paged output)
    // +the report itself
    container = new ReportContainer( messages );
    History.addValueChangeHandler( historyHandler );
    initUI();
    setupNativeHooks( this );
    hideParentReportViewers();
  }

  private void initUI() {
    final RootPanel panel = RootPanel.get( "content" ); //$NON-NLS-1$
    panel.clear();
    panel.add( container );
  }

  private native void setupNativeHooks( ReportViewer viewer )
  /*-{
    if ($wnd.reportViewer_openUrlInDialog || top.reportViewer_openUrlInDialog) {
      return;
    }
    if (!top.mantle_initialized) {
      top.mantle_openTab = function(name, title, url) {
        window.open(url, '_blank');
      }
    }
    if (top.mantle_initialized) {
      top.reportViewer_openUrlInDialog = function(title, url, width, height) {
        top.urlCommand(url, title, true, width, height);
      }
    } else {
      top.reportViewer_openUrlInDialog = function(title, url, width, height) {
        width += '';
        height += '';
        viewer.@org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer
          ::openUrlInDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)
           (title, url, width, height);
      }
    }
    
    $wnd.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
    $wnd.reportViewer_hide = function() {
      viewer.@org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer::hide()();
    };
  }-*/;

  private void hide() {
    container.hideParameterController();
  }

  private native void hideParentReportViewers()
  /*-{
    var count = 10;
    var myparent = $wnd.parent;
    while (myparent != null && count >= 0) {
      if ($wnd != myparent) {
        if(typeof myparent.reportViewer_hide == 'function') {
          myparent.reportViewer_hide();
        }
      }
      if (myparent == myparent.parent) {
        // BISERVER-3614:
        // while we don't know why this would ever be the case,
        // we know that this does happen and it will bring a browser
        // to its knees, it's as if top.parent == top
        break;
      }
      myparent = myparent.parent;
      if (myparent == top) {
        break;
      }
      count--;
    }
  }-*/;

  public void openUrlInDialog( final String title, final String url, String width, String height ) {
    if ( StringUtils.isEmpty( height ) ) {
      height = "600px"; //$NON-NLS-1$
    }
    if ( StringUtils.isEmpty( width ) ) {
      width = "800px"; //$NON-NLS-1$
    }
    if ( height.endsWith( "px" ) == false ) { //$NON-NLS-1$
      height += "px"; //$NON-NLS-1$
    }
    if ( width.endsWith( "px" ) == false ) { //$NON-NLS-1$
      width += "px"; //$NON-NLS-1$
    }

    final DialogBox dialogBox = new DialogBox( false, true );
    dialogBox.setStylePrimaryName( "pentaho-dialog" );
    dialogBox.setText( title );

    final Frame frame = new Frame( url );
    frame.setSize( width, height );

    final Button okButton = new Button( messages.getString( "ok", "OK" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    okButton.setStyleName( "pentaho-button" );
    okButton.addClickHandler( new ClickHandler() {
      public void onClick( final ClickEvent event ) {
        dialogBox.hide();
      }
    } );

    final HorizontalPanel buttonPanel = new HorizontalPanel();
    DOM.setStyleAttribute( buttonPanel.getElement(), "padding", "0px 5px 5px 5px" ); //$NON-NLS-1$ //$NON-NLS-2$
    buttonPanel.setWidth( "100%" ); //$NON-NLS-1$
    buttonPanel.setHorizontalAlignment( HasHorizontalAlignment.ALIGN_CENTER );
    buttonPanel.add( okButton );

    final VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute( dialogContent.getElement(), "padding", "0px 5px 0px 5px" ); //$NON-NLS-1$ //$NON-NLS-2$
    dialogContent.add( frame );
    dialogContent.add( buttonPanel );
    dialogBox.setWidget( dialogContent );

    // dialogBox.setHeight(height);
    // dialogBox.setWidth(width);
    dialogBox.center();
  }
}
