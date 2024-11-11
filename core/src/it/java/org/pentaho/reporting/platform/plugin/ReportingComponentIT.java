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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.test.platform.engine.core.MicroPlatform;

/**
 * Unit tests for the ReportingComponent.
 * 
 * @author Michael D'Amour
 */
public class ReportingComponentIT extends TestCase {

  private MicroPlatform microPlatform;
  private File tmp;

  @Override
  protected void setUp() throws Exception {
    tmp = new File("target/test/resource/solution/system/tmp");
    tmp.mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
  }

  public void testReportDefinitionAsInput() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream( "target/test/resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    Map<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( SimpleReportingComponent.REPORT_DEFINITION_INPUT, reportDefinition );
    rc.setInputs( inputs );
    rc.setOutputType( "application/pdf" ); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream( new File(tmp, System.currentTimeMillis() + ".pdf" )); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // validate the component
    assertTrue( rc.validate() );
    // execute the component
    assertTrue( rc.execute() );
  }

  public void testPDF() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream( "target/test/resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "application/pdf" ); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream( new File(tmp, System.currentTimeMillis() + ".pdf" )); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // validate the component
    assertTrue( rc.validate() );
    // execute the component
    assertTrue( rc.execute() );
  }

  public void testHTML() throws Exception {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream( "target/test/resource/solution/test/reporting/report.prpt" ); //$NON-NLS-1$
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream( new File(tmp, System.currentTimeMillis() + ".html" )); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // validate the component
    assertTrue( rc.validate() );
    // execute the component
    assertTrue( rc.execute() );
  }

}
