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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;

import junit.framework.TestCase;
import org.pentaho.platform.engine.services.actionsequence.ActionSequenceResource;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

public class PageableHTMLTest extends TestCase {

  SimpleReportingComponent rc;

  protected void setUp() {
    // create an instance of the component
    rc = new SimpleReportingComponent();
  }

  public void testSetPaginationAPI() throws Exception {
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
    rc.setAcceptedPage( 5 );

    // check the accepted page
    assertEquals( 5, rc.getAcceptedPage() );
  }

  public void testSetDefaultOutputTarget() throws Exception {
    String outputTarget = "output-target"; //$NON-NLS-1$

    rc.setDefaultOutputTarget( outputTarget );
    assertEquals( outputTarget, rc.getDefaultOutputTarget() );
  }

  public void testSetDefaultOutputTargetNull() throws Exception {
    try {
      rc.setDefaultOutputTarget( null );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
  }

  public void testSetForceDefaultOutputTarget() throws Exception {
    // make sure forceDefaultOutputTarget is not yet on
    assertEquals( false, rc.isForceDefaultOutputTarget() );

    rc.setForceDefaultOutputTarget( true );
    assertEquals( true, rc.isForceDefaultOutputTarget() );
  }

  public void testSetForceUnlockPreferredOutput() throws Exception {
    // make sure forceUnlockPreferredOutput is not yet on
    assertEquals( false, rc.isForceUnlockPreferredOutput() );

    rc.setForceUnlockPreferredOutput( true );
    assertEquals( true, rc.isForceUnlockPreferredOutput() );
  }

  public void testGetOutputTarget() throws Exception {
    final String outputTarget = "table/html;page-mode=stream"; //$NON-NLS-1$

    // make sure outputTarget is not yet on
    assertEquals( null, rc.getOutputTarget() );

    rc.setOutputTarget( outputTarget );
    assertEquals( outputTarget, rc.getOutputTarget() );
  }

  public void testSetOutputType() throws Exception {
    final String outputType = "text/html"; //$NON-NLS-1$

    // make sure outputType is not yet on
    assertEquals( null, rc.getOutputType() );

    rc.setOutputType( outputType );
    assertEquals( outputType, rc.getOutputType() );
  }

  public void testSetReportDefinition() throws Exception {
    // make sure reportDefinition is not yet on
    assertEquals( null, rc.getReportDefinition() );

    ActionSequenceResource asr = new ActionSequenceResource( "reportDefinition", 0, "", "" ); //$NON-NLS-1$

    rc.setReportDefinition( asr );
    assertEquals( asr, rc.getReportDefinition() );
  }

  public void testSetReportFileId() throws Exception {
    String fileId = "fileId"; //$NON-NLS-1$

    // make sure fileId is not yet on
    assertEquals( null, rc.getReportFileId() );

    rc.setReportFileId( fileId );
    assertEquals( fileId, rc.getReportFileId() );
  }

  public void testSetReportDefinitionPath() throws Exception {
    String definitionPath = "definition-path"; //$NON-NLS-1$

    // make sure reportDefinitionPath is not yet on
    assertEquals( null, rc.getReportDefinitionPath() );

    rc.setReportDefinitionPath( definitionPath );
    assertEquals( definitionPath, rc.getReportDefinitionPath() );
  }

  public void testSetDashboardMode() throws Exception {
    // make sure dashboardMode is not yet on
    assertEquals( false, rc.isDashboardMode() );

    rc.setDashboardMode( true );
    assertEquals( true, rc.isDashboardMode() );
  }

  public void testSetPrint() throws Exception {
    // make sure dashboardMode is not yet on
    assertEquals( false, rc.isPrint() );

    rc.setPrint( true );
    assertEquals( true, rc.isPrint() );
  }

  public void testSetPrinter() throws Exception {
    String printer = "printer"; //$NON-NLS-1$

    // make sure dashboardMode is not yet on
    assertEquals( null, rc.getPrinter() );

    rc.setPrinter( printer );
    assertEquals( printer, rc.getPrinter() );
  }

  public void testSetInputsEmpty() throws Exception {
    rc.setInputs( null );
    assertEquals( Collections.emptyMap(), rc.getInputs() );
  }

  public void testSetInputs() throws Exception {
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "3" ); //$NON-NLS-1$ //$NON-NLS-2$

    // make sure inputs is not yet filled
    assertEquals( Collections.emptyMap(), rc.getInputs() );
    rc.setInputs( inputs );

    assertEquals( inputs, rc.getInputs() );

    // Test several output types options
    inputs.clear();

    inputs.put( "output-type", "output-type" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "output-target", "output-target" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "paginate", true ); //$NON-NLS-1$
    inputs.put( "print", true ); //$NON-NLS-1$
    inputs.put( "printer-name", "printer-name" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "dashboard-mode", true ); //$NON-NLS-1$

    rc.setInputs( inputs );

    assertEquals( "output-type", rc.getOutputType() ); //$NON-NLS-1$
    assertEquals( "output-target", rc.getOutputTarget() ); //$NON-NLS-1$
    assertEquals( true, rc.isPaginateOutput() );
    assertEquals( true, rc.isPrint() );
    assertEquals( "printer-name", rc.getPrinter() ); //$NON-NLS-1$
    assertEquals( true, rc.isDashboardMode() );
  }

  public void testGetInput() throws Exception {
    HashMap<String, Object> inputs = new HashMap<String, Object>(); ;
    inputs.put( "paginate", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );
    assertEquals( "false", rc.getInput( "paginate", true ) ); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void testGetInputNull() throws Exception {
    assertEquals( true, rc.getInput( "paginate", true ) ); //$NON-NLS-1$
  }

  public void testGetPageCount() throws Exception {
    assertEquals( -1, rc.getPageCount() );
  }

  public void testValidate() throws Exception {
    assertEquals( false, rc.validate() );

    ActionSequenceResource asr = new ActionSequenceResource( "reportDefinition", 0, "", "" ); //$NON-NLS-1$
    rc.setReportDefinition( asr );
    rc.setReportFileId( "fileId" ); //$NON-NLS-1$
    rc.setReportDefinitionPath( "report-definition-path" );
    rc.setReportDefinitionInputStream( new ByteArrayInputStream( "test data".getBytes() ) );

    assertEquals( false, rc.validate() );

    rc.setOutputStream( new ByteArrayOutputStream() );
    rc.setPrint( true );

    rc.setInputs( new HashMap<String, Object>() );

    assertEquals( true, rc.validate() );
  }

  public void testOutputSupportsPaginationException() throws Exception {
    assertEquals( false, rc.outputSupportsPagination() );
  }

  public void testExecuteNoReportException() throws Exception {
    try {
      rc.execute();
    } catch ( ResourceException ex ) {
      assertTrue( true );
    }
  }

  public void testExecuteDummyReport() throws Exception {
    rc.setReport( new MasterReport() );
    assertFalse( rc.execute() );
  }

  public void testGetMimeType() throws Exception {
    rc.setReport( new MasterReport() );
    assertEquals( "text/html", rc.getMimeType() ); //$NON-NLS-1$
  }

  public void testGetMimeTypeGenericFallback() throws Exception {
    assertEquals( "application/octet-stream", rc.getMimeType() ); //$NON-NLS-1$
  }

  public void testPaginateInvalid() throws Exception {
    rc.setReport( new MasterReport() );
    assertEquals( 0, rc.paginate() );
  }

  public void testPaginateWithPrint() throws Exception {
    rc.setReport( new MasterReport() );
    rc.setPrint( true );
    assertEquals( 0, rc.paginate() );
  }
}
