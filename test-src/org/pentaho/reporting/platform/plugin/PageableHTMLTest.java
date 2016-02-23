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

import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.services.actionsequence.ActionSequenceResource;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.reporting.platform.plugin.output.CachingPageableHTMLOutput;
import org.pentaho.reporting.platform.plugin.output.FastExportReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class PageableHTMLTest {

  SimpleReportingComponent rc;
  private MicroPlatform microPlatform;
  private File tmp;
  private static FileSystemCacheBackend fileSystemCacheBackend;

  @BeforeClass
  public static void setUpClass() {
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
  }

  @AfterClass
  public static void tearDownClass() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
  }

  @Before
  public void setUp() throws Exception {
    // create an instance of the component
    rc = new SimpleReportingComponent();

    tmp = new File( "./resource/solution/system/tmp" );
    tmp.mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
    IPluginCacheManager iPluginCacheManager =
      new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) );
    microPlatform.define( "plugin-cache-manager", iPluginCacheManager );
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }


  @After
  public void tearDown() throws Exception {
    microPlatform.stop();
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
  public void testSetPageAPI() throws Exception {
    rc.setAcceptedPage( 5 );

    // check the accepted page
    assertEquals( 5, rc.getAcceptedPage() );
  }

  @Test
  public void testSetDefaultOutputTarget() throws Exception {
    String outputTarget = "output-target"; //$NON-NLS-1$

    rc.setDefaultOutputTarget( outputTarget );
    assertEquals( outputTarget, rc.getDefaultOutputTarget() );
  }

  @Test
  public void testSetDefaultOutputTargetNull() throws Exception {
    try {
      rc.setDefaultOutputTarget( null );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
  }

  @Test
  public void testSetForceDefaultOutputTarget() throws Exception {
    // make sure forceDefaultOutputTarget is not yet on
    assertEquals( false, rc.isForceDefaultOutputTarget() );

    rc.setForceDefaultOutputTarget( true );
    assertEquals( true, rc.isForceDefaultOutputTarget() );
  }

  @Test
  public void testSetForceUnlockPreferredOutput() throws Exception {
    // make sure forceUnlockPreferredOutput is not yet on
    assertEquals( false, rc.isForceUnlockPreferredOutput() );

    rc.setForceUnlockPreferredOutput( true );
    assertEquals( true, rc.isForceUnlockPreferredOutput() );
  }

  @Test
  public void testGetOutputTarget() throws Exception {
    final String outputTarget = "table/html;page-mode=stream"; //$NON-NLS-1$

    // make sure outputTarget is not yet on
    assertEquals( null, rc.getOutputTarget() );

    rc.setOutputTarget( outputTarget );
    assertEquals( outputTarget, rc.getOutputTarget() );
  }

  @Test
  public void testSetOutputType() throws Exception {
    final String outputType = "text/html"; //$NON-NLS-1$

    // make sure outputType is not yet on
    assertEquals( null, rc.getOutputType() );

    rc.setOutputType( outputType );
    assertEquals( outputType, rc.getOutputType() );
  }

  @Test
  public void testSetReportDefinition() throws Exception {
    // make sure reportDefinition is not yet on
    assertEquals( null, rc.getReportDefinition() );

    ActionSequenceResource asr = new ActionSequenceResource( "reportDefinition", 0, "", "" ); //$NON-NLS-1$

    rc.setReportDefinition( asr );
    assertEquals( asr, rc.getReportDefinition() );
  }

  @Test
  public void testSetReportFileId() throws Exception {
    String fileId = "fileId"; //$NON-NLS-1$

    // make sure fileId is not yet on
    assertEquals( null, rc.getReportFileId() );

    rc.setReportFileId( fileId );
    assertEquals( fileId, rc.getReportFileId() );
  }

  @Test
  public void testSetReportDefinitionPath() throws Exception {
    String definitionPath = "definition-path"; //$NON-NLS-1$

    // make sure reportDefinitionPath is not yet on
    assertEquals( null, rc.getReportDefinitionPath() );

    rc.setReportDefinitionPath( definitionPath );
    assertEquals( definitionPath, rc.getReportDefinitionPath() );
  }

  @Test
  public void testSetDashboardMode() throws Exception {
    // make sure dashboardMode is not yet on
    assertEquals( false, rc.isDashboardMode() );

    rc.setDashboardMode( true );
    assertEquals( true, rc.isDashboardMode() );
  }

  @Test
  public void testSetPrint() throws Exception {
    // make sure dashboardMode is not yet on
    assertEquals( false, rc.isPrint() );

    rc.setPrint( true );
    assertEquals( true, rc.isPrint() );
  }

  @Test
  public void testSetPrinter() throws Exception {
    String printer = "printer"; //$NON-NLS-1$

    // make sure dashboardMode is not yet on
    assertEquals( null, rc.getPrinter() );

    rc.setPrinter( printer );
    assertEquals( printer, rc.getPrinter() );
  }

  @Test
  public void testSetInputsEmpty() throws Exception {
    rc.setInputs( null );
    assertEquals( Collections.emptyMap(), rc.getInputs() );
  }

  @Test
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

  @Test
  public void testGetInput() throws Exception {
    HashMap<String, Object> inputs = new HashMap<String, Object>(); ;
    inputs.put( "paginate", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );
    assertEquals( "false", rc.getInput( "paginate", true ) ); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Test
  public void testGetInputNull() throws Exception {
    assertEquals( true, rc.getInput( "paginate", true ) ); //$NON-NLS-1$
  }

  @Test
  public void testGetPageCount() throws Exception {
    assertEquals( -1, rc.getPageCount() );
  }

  @Test
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

  @Test
  public void testOutputSupportsPaginationException() throws Exception {
    assertEquals( false, rc.outputSupportsPagination() );
  }

  @Test
  public void testExecuteNoReportException() throws Exception {
    try {
      rc.execute();
    } catch ( ResourceException ex ) {
      assertTrue( true );
    }
  }

  @Test
  public void testExecuteDummyReport() throws Exception {
    rc.setReport( new MasterReport() );
    assertFalse( rc.execute() );
  }

  @Test
  public void testGetMimeType() throws Exception {
    rc.setReport( new MasterReport() );
    assertEquals( "text/html", rc.getMimeType() ); //$NON-NLS-1$
  }

  @Test
  public void testGetMimeTypeGenericFallback() throws Exception {
    assertEquals( "application/octet-stream", rc.getMimeType() ); //$NON-NLS-1$
  }

  @Test
  public void testPaginateInvalid() throws Exception {
    rc.setReport( new MasterReport() );
    assertEquals( 0, rc.paginate() );
  }

  @Test
  public void testPaginateWithPrint() throws Exception {
    rc.setReport( new MasterReport() );
    rc.setPrint( true );
    assertEquals( 0, rc.paginate() );
  }

  @Test
  public void testPageCount() throws Exception {
    // create/set the InputStream
    FileInputStream reportDefinition =
      new FileInputStream( "resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    FileOutputStream outputStream =
      new FileOutputStream( new File( tmp, System.currentTimeMillis() + ".html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // execute the component
    assertTrue( rc.execute() );

    // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
    assertEquals( 8, rc.getPageCount() );
  }

  @Test
  public void testPaginatedHTML() throws Exception {
    // create/set the InputStream
    FileInputStream reportDefinition =
      new FileInputStream( "resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
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

    FileOutputStream outputStream =
      new FileOutputStream( new File( tmp, System.currentTimeMillis() + ".html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
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

  @Test
  public void testCaching() throws Exception {

    ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    try {
      ResourceManager mgr = new ResourceManager();
      File src = new File( "resource/solution/test/reporting/report.prpt" );
      MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      CachingPageableHTMLOutput out = new CachingPageableHTMLOutput();
      String key = out.createKey( r );

      // create an instance of the component
      SimpleReportingComponent rc = new SimpleReportingComponent();
      // create/set the InputStream
      rc.setReport( r );
      rc.setOutputType( "text/html" ); //$NON-NLS-1$

      // turn on pagination, by way of input (typical mode for xaction)
      HashMap<String, Object> inputs = new HashMap<String, Object>();
      inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
      inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
      rc.setInputs( inputs );

      FileOutputStream outputStream =
        new FileOutputStream( new File( tmp, System.currentTimeMillis() + ".html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
      rc.setOutputStream( outputStream );

      // execute the component
      assertTrue( rc.execute() );

      // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
      assertEquals( 8, rc.getPageCount() );

      // Check caching: PageNumbers
      final IReportContent cachedContent = out.getCachedContent( key );
      assertEquals( 8, cachedContent.getPageCount() );
      for ( int i = 0; i < 8; i++ ) {
        assertTrue( cachedContent.getPageData( i ) != null );
      }

    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
    }
  }


    @Test
    public void testCachingKeyGen() throws Exception {

      final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
      try {
        final ResourceManager mgr = new ResourceManager();
        final File src1 = new File( "resource/solution/test/reporting/report.prpt" );
        final File src2 = new File( "resource/solution/test/reporting/report1.prpt" );
        final MasterReport r1 = (MasterReport) mgr.createDirectly( src1, MasterReport.class ).getResource();
        final MasterReport r2 = (MasterReport) mgr.createDirectly( src2, MasterReport.class ).getResource();

        final CachingPageableHTMLOutput out = new CachingPageableHTMLOutput();
        final String key1 = out.createKey( r1 );
        final String sameKey1 = out.createKey( r1 );

        assertEquals( key1, sameKey1 );

        final String key2 = out.createKey( r2 );
        final String sameKey2 = out.createKey( r2 );

        assertEquals( key2, sameKey2 );

        assertNotSame( key2, key1 );

      } finally {
        edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      }
  }

}
