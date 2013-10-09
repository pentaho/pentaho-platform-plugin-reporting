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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class WaitPopup {

  private static final WaitPopup instance = new WaitPopup();
  private static FocusPanel pageBackground;
  private static int clickCount;

  public WaitPopup() {
    if ( pageBackground == null ) {
      pageBackground = new FocusPanel();
      pageBackground.setHeight( "100%" ); //$NON-NLS-1$
      pageBackground.setWidth( "100%" ); //$NON-NLS-1$
      pageBackground.setStyleName( "modalDialogPageBackground" ); //$NON-NLS-1$

      pageBackground.addClickHandler( new ClickHandler() {
        public void onClick( final ClickEvent event ) {
          clickCount++;
          if ( clickCount > 2 ) {
            clickCount = 0;
            pageBackground.setVisible( false );
          }
        }
      } );
      RootPanel.get().add( pageBackground, 0, 0 );
    }
  }

  public static WaitPopup getInstance() {
    return instance;
  }

  public void setVisible( final boolean visible ) {
    try {
      pageBackground.setVisible( visible );
    } catch ( Throwable t ) {
      // ignored
      ReportViewerUtil.checkStyleIgnore();
    }
  }

}
