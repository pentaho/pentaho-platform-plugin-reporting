package org.pentaho.reporting.platform.plugin.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.StagingHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Created by dima.prokopenko@gmail.com on 2/2/2016.
 */
public class PentahoAsyncReportExecution implements Callable<InputStream> {

  private static final Log log = LogFactory.getLog( PentahoAsyncReportExecution.class );

  private SimpleReportingComponent reportComponent;
  private StagingHandler handler;

  public PentahoAsyncReportExecution( SimpleReportingComponent reportComponent, StagingHandler handler )
      throws ResourceException, IOException {
    this.reportComponent = reportComponent;
    this.handler = handler;
  }

  @Override
  public InputStream call() throws Exception {
    //TODO copypaste from ExecuteReportContentHandler...

    final MasterReport report = reportComponent.getReport();
    //reportStagingHandler = new StagingHandler( outputStream, stagingMode, this.userSession );

    //...



    if ( reportComponent.execute() ) {
      // get input stream
    }

    //return input stream
    return null;
  }
}
