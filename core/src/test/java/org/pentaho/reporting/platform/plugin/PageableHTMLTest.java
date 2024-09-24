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
 * Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
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
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.engine.services.actionsequence.ActionSequenceResource;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.async.TestListener;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PageableHTMLTest {

  SimpleReportingComponent rc;
  private static MicroPlatform microPlatform;
  private File tmp;
  private static FileSystemCacheBackend fileSystemCacheBackend;
  private static IPluginCacheManager iPluginCacheManager;

  @BeforeClass
  public static void setUpClass() throws PlatformInitializationException {
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
    iPluginCacheManager =
      spy( new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) ) );
    microPlatform.define( "IPluginCacheManager", iPluginCacheManager );
    microPlatform.start();
  }

  @AfterClass
  public static void tearDownClass() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );

    microPlatform.stop();
    microPlatform = null;
  }

  @Before
  public void setUp() throws Exception {
    // create an instance of the component
    rc = new SimpleReportingComponent();

    tmp = new File( "target/test/resource/solution/system/tmp" );
    tmp.mkdirs();


    final IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );

    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
  }


  @After
  public void tearDown() throws Exception {
    reset( iPluginCacheManager );
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
      new FileInputStream( "target/test/resource/solution/test/reporting/report1.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    File file = new File( tmp, System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    FileOutputStream outputStream = new FileOutputStream( file );
    rc.setOutputStream( outputStream );

    try {
      // execute the component
      assertTrue( rc.execute() );

      // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
      assertEquals(8, rc.getPageCount());
    } finally {
      reportDefinition.close();
      outputStream.close();
      if ( file.exists() ) {
        file.delete();
      }
    }
  }

  @Test
  public void testPaginatedHTML() throws Exception {
    // create/set the InputStream
    FileInputStream reportDefinition =
      new FileInputStream( "target/test/resource/solution/test/reporting/report1.prpt" ); //$NON-NLS-1$
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

    File file = new File( tmp, System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    FileOutputStream outputStream = new FileOutputStream( file );
    rc.setOutputStream( outputStream );

    // check the accepted page
    assertEquals( 0, rc.getAcceptedPage() );

    // make sure pagination is really on
    assertTrue( rc.isPaginateOutput() );

    try {
      // validate the component
      assertTrue( rc.validate() );

      // execute the component
      assertTrue( rc.execute() );

      // make sure this report has 8 pages (we know this report will produce 8 pages with sample data)
      assertEquals(8, rc.getPageCount());
    } finally {
        reportDefinition.close();
        outputStream.close();
        if ( file.exists() ) {
          file.delete();
        }
    }
  }

  @Test
  public void testCaching() throws Exception {

    File file = null;
    FileOutputStream outputStream = null;
    ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    try {
      ResourceManager mgr = new ResourceManager();
      File src = new File( "target/test/resource/solution/test/reporting/report1.prpt" );
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

      file = new File( tmp, System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
      outputStream = new FileOutputStream( file );
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
      if ( null != outputStream ) {
        outputStream.close();
      }
      if ( null != file && file.exists() ) {
        file.delete();
      }
    }
  }


  @Test
  public void testCachingKeyGen() throws Exception {

    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    try {
      final ResourceManager mgr = new ResourceManager();
      final File src1 = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final File src2 = new File( "target/test/resource/solution/test/reporting/report1.prpt" );
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

  @Test
  public void testListener() throws Exception {

    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report1.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      execute( r );

      assertTrue( listener.isOnStart() );
      assertTrue( listener.isOnUpdate() );
      assertTrue( listener.isOnFinish() );
      assertTrue( listener.isOnFirstPage() );
      assertFalse( -1 == listener.getState().getRow() );
      assertFalse( -1 == listener.getState().getTotalRows() );
      assertEquals( 1, listener.getState().getGeneratedPage() );

      ReportListenerThreadHolder.clear();

      assertNull( ReportListenerThreadHolder.getListener() );

      final MasterReport r2 = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      final TestListener listener2 = new TestListener( "2", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener2 );

      //From cache
      execute( r2 );

      assertFalse( listener2.isOnStart() );
      assertTrue( listener2.isOnUpdate() );
      assertTrue( listener2.isOnFinish() );
      assertFalse( listener2.isOnFirstPage() );

    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }


  @Test
  public void testListenerFirstPageOff() throws Exception {

    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "false" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      execute( r );

      assertTrue( listener.isOnStart() );
      assertTrue( listener.isOnUpdate() );
      assertTrue( listener.isOnFinish() );
      assertFalse( listener.isOnFirstPage() );
      assertFalse( -1 == listener.getState().getRow() );
      assertFalse( -1 == listener.getState().getTotalRows() );

      ReportListenerThreadHolder.clear();

      assertNull( ReportListenerThreadHolder.getListener() );

      final MasterReport r2 = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      final TestListener listener2 = new TestListener( "2", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener2 );

      //From cache
      execute( r2 );

      assertFalse( listener2.isOnStart() );
      assertTrue( listener2.isOnUpdate() );
      assertTrue( listener2.isOnFinish() );
      assertFalse( listener2.isOnFirstPage() );

    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }


  @Test
  public void testRequestPage() throws Exception {

    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      final int requestedPage = 3;
      listener.setRequestedPage( requestedPage );
      assertEquals( requestedPage, listener.getRequestedPage() );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report1.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      execute( r );

      assertEquals( 0, listener.getRequestedPage() );
      assertEquals( requestedPage + 1, listener.getState().getGeneratedPage() );


      final MasterReport r2 = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      final TestListener listener2 = new TestListener( "2", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener2 );

      //From cache
      execute( r2 );


      assertEquals( 0, listener2.getRequestedPage() );
      assertEquals( 8, listener2.getState().getGeneratedPage() );

      ReportListenerThreadHolder.clear();

      assertNull( ReportListenerThreadHolder.getListener() );

    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }


  @Test
  public void testSchedule() throws Exception {
    doTestSchedule( "true" );
    doTestSchedule( "false" );
  }

  private void doTestSchedule( String cacheable ) throws Exception {
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", cacheable );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      listener.setStatus( AsyncExecutionStatus.SCHEDULED );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();

      // create an instance of the component
      final SimpleReportingComponent rc = new SimpleReportingComponent();
      // create/set the InputStream
      rc.setReport( r );
      rc.setOutputType( "text/html" ); //$NON-NLS-1$

      // turn on pagination, by way of input (typical mode for xaction)
      final HashMap<String, Object> inputs = new HashMap<String, Object>();
      inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
      inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
      rc.setInputs( inputs );

      try ( final ByteArrayOutputStream outputStream =
              new ByteArrayOutputStream() ) { //$NON-NLS-1$ //$NON-NLS-2$
        rc.setOutputStream( outputStream );
        assertTrue( rc.execute() );
        final byte[] bytes = outputStream.toByteArray();
        assertNotNull( bytes );
        final String content = new String( bytes, "UTF-8" );
        assertTrue( content.contains( "Scheduled paginated HTML report" ) );
      }

      // execute the component
      assertTrue( listener.isOnStart() );
      assertTrue( listener.isOnUpdate() );
      assertTrue( listener.isOnFinish() );

      assertFalse( -1 == listener.getState().getRow() );
      assertFalse( -1 == listener.getState().getTotalRows() );

      ReportListenerThreadHolder.clear();

      assertNull( ReportListenerThreadHolder.getListener() );

      //From cache
      final TestListener listener2 = new TestListener( "1", UUID.randomUUID(), "" );
      listener2.setStatus( AsyncExecutionStatus.SCHEDULED );
      ReportListenerThreadHolder.setListener( listener2 );

      final SimpleReportingComponent rc2 = new SimpleReportingComponent();
      // create/set the InputStream
      rc.setReport( r );
      rc.setOutputType( "text/html" ); //$NON-NLS-1$

      rc.setInputs( inputs );

      try ( final ByteArrayOutputStream outputStream =
              new ByteArrayOutputStream() ) { //$NON-NLS-1$ //$NON-NLS-2$
        rc.setOutputStream( outputStream );
        assertTrue( rc.execute() );
        final byte[] bytes = outputStream.toByteArray();
        assertNotNull( bytes );
        final String content = new String( bytes, "UTF-8" );
        assertTrue( content.contains( "Scheduled paginated HTML report" ) );
      }

      // execute the component
      assertTrue( listener.isOnStart() );
      assertTrue( listener.isOnUpdate() );
      assertTrue( listener.isOnFinish() );

      assertFalse( -1 == listener.getState().getRow() );
      assertFalse( -1 == listener.getState().getTotalRows() );

      ReportListenerThreadHolder.clear();


    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

  @Test
  public void testPaginate() throws Exception {
    // create/set the InputStream
    final FileInputStream reportDefinition =
      new FileInputStream( "target/test/resource/solution/test/reporting/report1.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    final HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "-1" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    File file = new File( tmp, System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    final FileOutputStream outputStream = new FileOutputStream( file );
    rc.setOutputStream( outputStream );

    try {
      assertEquals(  8, rc.paginate() );
    } finally {
      reportDefinition.close();
      outputStream.close();
      if ( file.exists() ) {
        file.delete();
      }
    }

  }

  @Test
  public void testEmpyReport() throws ContentIOException, ReportProcessingException, IOException {
    final CachingPageableHTMLOutput cachingPageableHTMLOutput = spy( new CachingPageableHTMLOutput() );
    cachingPageableHTMLOutput.generate( new MasterReport(), 1, mock( OutputStream.class ), 1 );
    cachingPageableHTMLOutput.paginate( new MasterReport(), 1 );
    //Non caching way
    verify( iPluginCacheManager, never() ).getCache();
  }


  @Test
  public void testAlreadyWithCacheKey() throws Exception {

    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report1.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      r.setContentCacheKey( "somekey" );

      execute( r );

      assertTrue( listener.isOnStart() );
      assertTrue( listener.isOnUpdate() );
      assertTrue( listener.isOnFinish() );
      assertTrue( listener.isOnFirstPage() );
      assertFalse( -1 == listener.getState().getRow() );
      assertFalse( -1 == listener.getState().getTotalRows() );
      assertEquals( 1, listener.getState().getGeneratedPage() );

      ReportListenerThreadHolder.clear();

      assertNull( ReportListenerThreadHolder.getListener() );

    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

  private void execute( final MasterReport r ) throws Exception {
    // create an instance of the component
    final SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    rc.setReport( r );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    final HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "content-handler-pattern", "test" );
    rc.setInputs( inputs );

    File file = new File( tmp, System.currentTimeMillis() + ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    final FileOutputStream outputStream = new FileOutputStream( file );
    rc.setOutputStream( outputStream );

    try {
      // execute the component
      assertTrue( rc.execute() );
    } finally {
      outputStream.close();
      if ( file.exists() ) {
        file.delete();
      }
    }
  }


}
