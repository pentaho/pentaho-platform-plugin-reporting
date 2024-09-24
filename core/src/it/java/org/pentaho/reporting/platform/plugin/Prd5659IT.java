/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.output.FastStreamHtmlOutput;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;

public class Prd5659IT {
  private static MicroPlatform microPlatform;

  private static File tmp;

  @AfterClass
  public static void tearDownClass() throws IOException {
    FileUtils.deleteDirectory( new File( "target/test/resource/solution/system/tmp/async" ) );
    microPlatform.stop();
    microPlatform = null;
  }

  @BeforeClass
  public static void setUp() throws Exception {
    tmp = new File( "target/test/resource/solution/system/tmp/async" );
    tmp.mkdirs();
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }


  @Test
  public void testHtmlSinglePageModeFailExecution() throws Exception {

    FileInputStream reportDefinition =
      new FileInputStream( "target/test/resource/solution/test/reporting/BigReport.prpt" ); //$NON-NLS-1$

    // create an instance of the component
    SimpleReportingComponent rc = spy( new SimpleReportingComponent() );
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setForceDefaultOutputTarget( true );
    rc.setPaginateOutput( true );
    rc.getReport().getReportConfiguration()
      .setConfigProperty( ExecuteReportContentHandler.FORCED_BUFFERED_WRITING, "false" );

    FastStreamHtmlOutput fastStreamHtmlOutput = new FastStreamHtmlOutput( "/pentaho/getImage?image={0}" );
    stub( rc.createOutputHandlerForOutputType( any( String.class ) ) ).toReturn( fastStreamHtmlOutput );

    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "page-mode", "stream" );
    inputs.put( "accepted-page", "-1" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );


    File file = new File( tmp, System.currentTimeMillis() + ".tmp" );
    FileOutputStream outputStream =
      new FileOutputStream( file ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // execute the component
    assertTrue( rc.execute() );

    // make sure this report don't write to file
    assertTrue( file.exists() );
    assertTrue( FileUtils.sizeOf( file ) == 0 );
    outputStream.close();
  }

  @Test
  public void testHtmlSinglePageModeSuccessExecution() throws Exception {

    FileInputStream reportDefinition =
      new FileInputStream( "target/test/resource/solution/test/reporting/BigReport.prpt" ); //$NON-NLS-1$

    // create an instance of the component
    SimpleReportingComponent rc = spy( new SimpleReportingComponent() );
    rc.setReportDefinitionInputStream( reportDefinition );
    rc.setForceDefaultOutputTarget( true );
    rc.setPaginateOutput( true );
    rc.getReport().getReportConfiguration()
      .setConfigProperty( ExecuteReportContentHandler.FORCED_BUFFERED_WRITING, "true" );

    FastStreamHtmlOutput fastStreamHtmlOutput = new FastStreamHtmlOutput( "/pentaho/getImage?image={0}" );
    stub( rc.createOutputHandlerForOutputType( any( String.class ) ) ).toReturn( fastStreamHtmlOutput );

    HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "page-mode", "stream" );
    inputs.put( "accepted-page", "-1" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    File file = new File( tmp, System.currentTimeMillis() + ".tmp" );

    FileOutputStream outputStream =
      new FileOutputStream( file ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // execute the component
    assertTrue( rc.execute() );

    // make sure this report write to file
    assertTrue( file.exists() );
    assertTrue( FileUtils.sizeOf( file ) > 0 );
    outputStream.close();
  }

}
