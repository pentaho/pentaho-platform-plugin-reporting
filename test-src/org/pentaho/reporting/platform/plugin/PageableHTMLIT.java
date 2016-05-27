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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.async.TestListener;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class PageableHTMLIT {
  private static MicroPlatform microPlatform;
  private static File tmp;
  private static FileSystemCacheBackend fileSystemCacheBackend;

  @BeforeClass
  public static void setUpClass() throws PlatformInitializationException {
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
    tmp = new File( "./resource/solution/system/tmp" );
    tmp.mkdirs();

    microPlatform = MicroPlatformFactory.create();
    IPluginCacheManager iPluginCacheManager =
      new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) );
    microPlatform.define( "IPluginCacheManager", iPluginCacheManager );
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @AfterClass
  public static void tearDownClass() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
    microPlatform.stop();
    microPlatform = null;
  }


  @Test
  public void testPageCount() throws Exception {

    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
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
    doTestPaginatedHTML( true, false );
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
    doTestPaginatedHTML( true, true );
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
    doTestPaginatedHTML( false, true );
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
    doTestPaginatedHTML( false, false );
  }


  public void doTestPaginatedHTML( final boolean cache, final boolean listener ) throws Exception {
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent",
      String.valueOf( cache ) );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.engine.classic.core.states.PerformanceMonitorContext",
      "org.pentaho.reporting.platform.plugin.PluginPerfomanceMonitorContext" );
    try {

      if ( listener ) {
        ReportListenerThreadHolder.setListener( new TestListener( "test", UUID.randomUUID(), "text/html" ) );
      }

      // create an instance of the component
      SimpleReportingComponent rc = new SimpleReportingComponent();
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
    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
      if ( listener ) {
        ReportListenerThreadHolder.clear();
      }
    }

  }


  @Test
  public void testPaginate() throws Exception {
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {
      // create an instance of the component
      SimpleReportingComponent rc = new SimpleReportingComponent();
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

      assertEquals( 8, rc.paginate() );
    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

}
