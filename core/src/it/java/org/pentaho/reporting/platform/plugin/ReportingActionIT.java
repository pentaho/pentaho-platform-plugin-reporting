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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.repository2.unified.fileio.RepositoryFileInputStream;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the ReportingComponent.
 * 
 * @author Michael D'Amour
 */
public class ReportingActionIT {

  private static MicroPlatform microPlatform;
  private static File tempFile;
  private static String PATH = "target/test/resource/solution/test/reporting/report.prpt";

  @BeforeClass
  public static void setUp() throws Exception {
    tempFile = new File( "target/test/resource/solution/system/tmp" );
    tempFile.mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @AfterClass
  public static void tearDown() throws Exception {
    microPlatform.stop();
    tempFile.delete();
    microPlatform = null;
  }

  @Test
  public void convertToPdf_ViaFileInputStream() throws Exception {
    FileInputStream fileInputStream = new FileInputStream( PATH ); //$NON-NLS-1$
    convertToPdf( fileInputStream );
  }

  @Test
  public void convertToPdf_ViaRepositoryFileInputStream() throws Exception {
    RepositoryFileInputStream repositoryFileInputStream = new RepositoryFileInputStream( PATH ); //$NON-NLS-1$
    convertToPdf( repositoryFileInputStream );
  }

  public void convertToPdf( InputStream stream ) throws Exception {
    final SimpleReportingAction reportingAction = new SimpleReportingAction();
    reportingAction.setInputStream( stream );
    reportingAction.setOutputType( "application/pdf" ); //$NON-NLS-1$

    File outputFile = File.createTempFile( "reportingActionTestResult", ".pdf" ); //$NON-NLS-1$ //$NON-NLS-2$
    System.out.println( "Test output located at: " + outputFile.getAbsolutePath() ); //$NON-NLS-1$
    FileOutputStream outputStream = new FileOutputStream( outputFile );
    reportingAction.setOutputStream( outputStream );

    // validate the component
    assertTrue( reportingAction.validate() );
    SecurityHelper.getInstance().runAsUser( "joe", new Callable<Object>() { //$NON-NLS-1$
      public Object call() throws Exception {
        try {
          reportingAction.execute();
        } catch ( Exception e ) {
          e.printStackTrace();
          Assert.fail();
        }
        return null;
      }

    } );

    assertTrue( outputFile.exists() );
  }

  // public void testHTML() throws Exception
  // {
  // // create an instance of the component
  // SimpleReportingComponent rc = new SimpleReportingComponent();
  // // create/set the InputStream
  //    FileInputStream reportDefinition = new FileInputStream("resource/solution/test/reporting/report.prpt"); //$NON-NLS-1$
  // rc.setReportDefinitionInputStream(reportDefinition);
  //    rc.setOutputType("text/html"); //$NON-NLS-1$
  //
  //    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".html"); //$NON-NLS-1$ //$NON-NLS-2$
  // rc.setOutputStream(outputStream);
  //
  // // validate the component
  // assertTrue(rc.validate());
  // // execute the component
  // assertTrue(rc.execute());
  // }

}
