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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.platform.plugin.output.FastStreamHtmlOutput;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;

public class Prd5659IT {
  private MicroPlatform microPlatform;
  private File tmp;

  @AfterClass
  public static void tearDownClass() throws IOException {
    FileUtils.deleteDirectory( new File( "./resource/solution/system/tmp/async" ) );
  }

  @Before
  public void setUp() throws Exception {
    tmp = new File( "./resource/solution/system/tmp/async" );
    tmp.mkdirs();
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }

  @After
  public void tearDown() throws Exception {
    microPlatform.stop();
  }

  @Test
  public void testHtmlSinglePageModeFailExecution() throws Exception {

    FileInputStream reportDefinition =
      new FileInputStream( "resource/solution/test/reporting/BigReport.prpt" ); //$NON-NLS-1$

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
      new FileInputStream( "resource/solution/test/reporting/BigReport.prpt" ); //$NON-NLS-1$

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
