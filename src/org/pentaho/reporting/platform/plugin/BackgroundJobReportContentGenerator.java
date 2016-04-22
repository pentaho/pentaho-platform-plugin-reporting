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

package org.pentaho.reporting.platform.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncReportExecution;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * push future task to executor and send redirect immediately.
 * <p/>
 * Created by dima.prokopenko@gmail.com on 2/4/2016.
 */
public class BackgroundJobReportContentGenerator extends ParameterContentGenerator {

  private static final Log logger = LogFactory.getLog( BackgroundJobReportContentGenerator.class );

  interface HttpServletResponse102 extends HttpServletResponse {
    // Processing (WebDAV; RFC 2518)
    int SC_PROCESSING = 102;
  }

  public static final String REDIRECT_PREFIX = "/pentaho/plugin/reporting/api/jobs/";
  public static final String REDIRECT_POSTFIX = "/status";

  @Override
  public void createContent( OutputStream outputStream ) throws Exception {
    final IParameterProvider requestParams = getRequestParameters();
    RepositoryFile prptFile = resolvePrptFile( requestParams );
    // we don't write directly for servlet output stream for async mode
    // typically we send redirect or error using HttpServletResponse mechanism
    this.createReportContent( prptFile.getId(), prptFile.getPath() );
  }

  public void createReportContent( final Serializable fileId, final String path )
    throws Exception {
    this.createReportContent( fileId, path, new SimpleReportingComponent(), new AuditWrapper() );
  }

  public void createReportContent( final Serializable fileId, final String path,
                            final SimpleReportingComponent reportComponent,
                            AuditWrapper audit )
    throws Exception {
    final Map<String, Object> inputs = this.createInputs();

    // set instance id to be accessible for debug (if debug is enabled)
    ReportListenerThreadHolder.setRequestId( this.instanceId );

    // register execution attempt
    audit.audit( userSession.getId(), userSession.getName(), path, getObjectName(), getClass().getName(),
      MessageTypes.EXECUTION, instanceId, "", 0, this );

    // prepare execution, copy-paste from ExecuteReportContentHandler.
    reportComponent.setReportFileId( fileId );
    reportComponent.setPaginateOutput( true );
    reportComponent.setForceDefaultOutputTarget( false );
    reportComponent.setDefaultOutputTarget( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE );
    if ( path.endsWith( ".prpti" ) ) {
      reportComponent.setForceUnlockPreferredOutput( true );
    }
    reportComponent.setInputs( inputs );

    AsyncJobFileStagingHandler handler = new AsyncJobFileStagingHandler( userSession );
    // will write to async stage target
    reportComponent.setOutputStream( handler.getStagingOutputStream() );

    PentahoAsyncReportExecution
      asyncExec = new PentahoAsyncReportExecution( path, reportComponent, handler, userSession, instanceId, audit );

    IPentahoAsyncExecutor executor =
      PentahoSystem.get( PentahoAsyncExecutor.class, PentahoAsyncExecutor.BEAN_NAME, null );
    // delegation
    if ( reportComponent.validate() ) {
      UUID uuid = executor.addTask( asyncExec, userSession );
      sendSuccessRedirect( uuid );
    } else {
      // register failed parameters execution attempt
      audit.audit( userSession.getId(), userSession.getName(), path, getObjectName(), getClass().getName(),
        MessageTypes.FAILED, instanceId, "", 0, this );
      sendErrorResponse();
    }
  }

  protected void sendErrorResponse() throws IOException {
    HttpServletResponse httpResponse = getServletResponse();
    httpResponse.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
  }

  protected void sendSuccessRedirect( UUID uuid ) throws IOException {
    HttpServletResponse httpResponse = getServletResponse();
    httpResponse.setStatus( HttpServletResponse102.SC_PROCESSING );
    httpResponse.sendRedirect( REDIRECT_PREFIX + uuid.toString() + REDIRECT_POSTFIX );
  }

  protected HttpServletResponse getServletResponse() {
    final IParameterProvider pathProviders = parameterProviders.get( "path" );
    final Object httpResponseObj = pathProviders.getParameter( "httpresponse" );
    HttpServletResponse httpResponse = HttpServletResponse.class.cast( httpResponseObj );
    return httpResponse;
  }

  @Override
  public Log getLogger() {
    return logger;
  }

  @Override
  public String getMimeType() {
    return "text/html";
  }
}

