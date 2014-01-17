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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import junit.framework.TestCase;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.test.platform.engine.core.MicroPlatform;

public class PageableHTMLTest extends TestCase {
  private MicroPlatform microPlatform;

  @Override
  protected void setUp() throws Exception {
    new File( "./resource/solution/system/tmp" ).mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
  }

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

  public void testPageCount() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream( "resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    FileOutputStream outputStream = new FileOutputStream( "/tmp/" + System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // execute the component
    assertTrue( rc.execute() );

    // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
    assertEquals( 8, rc.getPageCount() );
  }

  public void testPaginatedHTML() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream( "resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination
    rc.setPaginateOutput( true );
    assertTrue( rc.isPaginateOutput() );

    // turn it back off
    rc.setPaginateOutput( false );
    assertFalse( rc.isPaginateOutput() );

    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    FileOutputStream outputStream = new FileOutputStream( "/tmp/" + System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // check the accepted page
    assertEquals( 0, rc.getAcceptedPage() );

    // make sure pagination is really on
    assertTrue( rc.isPaginateOutput() );
    // validate the component
    assertTrue( rc.validate() );

    // execute the component
    assertTrue( rc.execute() );

    // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
    assertEquals( 8, rc.getPageCount() );

  }

}
