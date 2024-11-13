/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IOutputHandler;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoUrlFactory;
import org.pentaho.platform.api.engine.IRuntimeContext;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.plugin.services.messages.Messages;
import org.pentaho.platform.util.web.SimpleUrlFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

/**
 * Integration tests for the ReportingComponent.
 * 
 * @author David Kincade
 */
public class ReportingComponentIntegrationIT extends TestCase {

  private MicroPlatform microPlatform;
  // Logger
  private static final Log log = LogFactory.getLog( ReportingComponentIntegrationIT.class );

  @Override
  protected void setUp() throws Exception {
    new File( "target/test/resource/solution/system/tmp" ).mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
    microPlatform = null;
  }

  @Test
  public void test1_pdf() {
    SimpleParameterProvider parameterProvider = new SimpleParameterProvider();
    parameterProvider.setParameter( "outputType", "application/pdf" ); //$NON-NLS-1$ //$NON-NLS-2$
    OutputStream outputStream = getOutputStream( "ReportingTest.test1", ".pdf" ); //$NON-NLS-1$ //$NON-NLS-2$
    SimpleOutputHandler outputHandler = new SimpleOutputHandler( outputStream, true );
    StandaloneSession session =
        new StandaloneSession( Messages.getInstance().getString( "BaseTest.DEBUG_JUNIT_SESSION" ) ); //$NON-NLS-1$
    IRuntimeContext context =
        run( "target/test/resource/solution/test/reporting/test1.xaction", null, false, parameterProvider, outputHandler, session ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertEquals(
        Messages.getInstance().getString( "BaseTest.USER_RUNNING_ACTION_SEQUENCE" ), IRuntimeContext.RUNTIME_STATUS_SUCCESS, context.getStatus() ); //$NON-NLS-1$
  }

  @Test
  public void test1_html() {
    SimpleParameterProvider parameterProvider = new SimpleParameterProvider();
    parameterProvider.setParameter( "outputType", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    OutputStream outputStream = getOutputStream( "ReportingTest.test1", ".html" ); //$NON-NLS-1$ //$NON-NLS-2$
    SimpleOutputHandler outputHandler = new SimpleOutputHandler( outputStream, true );
    StandaloneSession session =
        new StandaloneSession( Messages.getInstance().getString( "BaseTest.DEBUG_JUNIT_SESSION" ) ); //$NON-NLS-1$
    IRuntimeContext context =
        run( "target/test/resource/solution/test/reporting/test1.xaction", null, false, parameterProvider, outputHandler, session ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertEquals(
        Messages.getInstance().getString( "BaseTest.USER_RUNNING_ACTION_SEQUENCE" ), IRuntimeContext.RUNTIME_STATUS_SUCCESS, context.getStatus() ); //$NON-NLS-1$
  }

  protected OutputStream getOutputStream( String testName, String extension ) {
    OutputStream outputStream = null;
    try {
      String tmpDir = PentahoSystem.getApplicationContext().getFileOutputPath( "test/tmp" ); //$NON-NLS-1$
      File file = new File( tmpDir );
      file.mkdirs();
      String path = PentahoSystem.getApplicationContext().getFileOutputPath( "test/tmp/" + testName + extension ); //$NON-NLS-1$
      outputStream = new FileOutputStream( path );
    } catch ( FileNotFoundException e ) {
      CommonUtil.checkStyleIgnore();
    }
    return outputStream;
  }

  public IRuntimeContext run( String actionPath, String instanceId, boolean persisted,
      IParameterProvider parameterProvider, IOutputHandler outputHandler, IPentahoSession session ) {
    List<String> messages = new ArrayList<String>();
    String baseUrl = ""; //$NON-NLS-1$
    HashMap<String, IParameterProvider> parameterProviderMap = new HashMap<String, IParameterProvider>();
    parameterProviderMap.put( IParameterProvider.SCOPE_REQUEST, parameterProvider );
    ISolutionEngine solutionEngine = PentahoSystem.get( ISolutionEngine.class, session );

    IPentahoUrlFactory urlFactory = new SimpleUrlFactory( baseUrl );

    IRuntimeContext context =
        solutionEngine.execute( actionPath,
            "", false, true, instanceId, persisted, parameterProviderMap, outputHandler, null, urlFactory, messages ); //$NON-NLS-1$

    return context;
  }
}
