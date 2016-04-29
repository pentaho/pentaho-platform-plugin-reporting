/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */


package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.OutputStream;

public class PentahoAsyncReportExecution extends AbstractAsyncReportExecution<IAsyncReportState> {

  private static final Log log = LogFactory.getLog( PentahoAsyncReportExecution.class );

  public PentahoAsyncReportExecution( final String url,
                                      final SimpleReportingComponent reportComponent,
                                      final AsyncJobFileStagingHandler handler,
                                      final IPentahoSession safeSession,
                                      final String auditId ) {
    super( url, reportComponent, handler, safeSession, auditId );
  }


  /**
   * Generate report and return input stream to a generated report from server.
   * <p>
   * Pay attention - it is important to set proper status during execution. In case 'fail' or 'complete' status not set
   * - status remains 'working' and executor unable to determine that actual execution has ended.
   *
   * @return input stream for client
   * @throws Exception
   */
  @Override public IFixedSizeStreamingContent call() throws Exception {
    final AsyncReportStatusListener listener = getListener();
    if ( listener == null ) {
      throw new NullPointerException( "No listener for async report execution: " + url );
    }

    try {
      listener.setStatus( AsyncExecutionStatus.WORKING );

      PentahoSessionHolder.setSession( safeSession );

      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();
      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
        MessageTypes.INSTANCE_START, auditId, "", 0, null );

      if ( reportComponent.execute() ) {

        final long end = System.currentTimeMillis();
        AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.INSTANCE_END, auditId, "", ( (float) ( end - start ) / 1000 ), null );


        final IFixedSizeStreamingContent stagingContent = handler.getStagingContent();

        listener.setStatus( AsyncExecutionStatus.FINISHED );

        return stagingContent;
      }

      // in case execute just returns false without an exception.
      fail();
      return NULL;
    } catch ( final Throwable ee ) {
      // it is bad practice to catch throwable.
      // but we has to to set proper execution status in any case.
      // Example: NoSuchMethodError (instance of Error) in case of usage of
      // uncompilable jar versions.
      // We have to avoid to hang on working status.
      log.error( "fail to execute report in async mode: " + ee );
      // to be sure after an error output stream is closed
      IOUtils.closeQuietly( handler.getStagingOutputStream() );
      if ( ee.getMessage() != null ) {
        listener.setErrorMessage( ee.getMessage() );
      }
      fail();
      return NULL;
    } finally {
      // in case report processor not going to close it
      OutputStream out = handler.getStagingOutputStream();
      IOUtils.closeQuietly( out );

      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.removeSession();
    }
  }

  @Override public String toString() {
    return "PentahoAsyncReportExecution{" + "url='" + url + '\'' + ", instanceId='" + auditId + '\'' + ", listener="
        + getListener() + '}';
  }

  @Override public IAsyncReportState getState() {
    final AsyncReportStatusListener listener = getListener();
    if ( listener == null ) {
      throw new IllegalStateException( "Cannot query state until job is added to the executor." );
    }
    return listener.getState();
  }
}
