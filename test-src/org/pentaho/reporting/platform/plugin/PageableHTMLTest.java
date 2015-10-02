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

package org.pentaho.reporting.platform.plugin;

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Created by webdetails on 02/10/2015.
 */
public class PageableHTMLTest extends TestCase {

  public void testSetPaginationAPI() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();

    // make sure pagination is not yet on
    assertFalse( rc.isPaginateOutput() );

    // turn on pagination
    rc.setPaginateOutput( true );
    assertTrue( rc.isPaginateOutput() );

    // turn it back off
    rc.setPaginateOutput( false );
    assertFalse( rc.isPaginateOutput() );
  }

  public void testSetPaginationFromInputs() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();

    // make sure pagination is not yet on
    assertFalse( rc.isPaginateOutput() );

    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    assertTrue( rc.isPaginateOutput() );

    // turn it back off
    rc.setPaginateOutput( false );
    assertFalse( rc.isPaginateOutput() );
  }

  public void testSetPageFromInputs() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    rc.setPaginateOutput( true );

    // make sure pagination is not yet on
    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "3" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    // check the accepted page
    assertEquals( 3, rc.getAcceptedPage() );
  }

  public void testSetPageAPI() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    rc.setAcceptedPage( 5 );

    // check the accepted page
    assertEquals( 5, rc.getAcceptedPage() );
  }

}
