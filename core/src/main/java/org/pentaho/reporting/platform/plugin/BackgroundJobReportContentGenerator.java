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
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.platform.plugin.async.IJobIdGenerator;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncReportExecution;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import javax.servlet.http.HttpServletRequest;
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
  private static final String PRPTI = ".prpti";
  private static final String RESERVED_ID = "reservedId";
  private static final String PATH = "path";
  private static final String HTTPRESPONSE = "httpresponse";
  private static final String HTTPREQUEST = "httprequest";

  interface HttpServletResponse102 extends HttpServletResponse {
    // Processing (WebDAV; RFC 2518)
    int SC_PROCESSING = 102;
  }

  static final String REDIRECT_PREFIX = "/plugin/reporting/api/jobs/";
  static final String REDIRECT_POSTFIX = "/status";

  @Override
  public void createContent( final OutputStream outputStream ) throws Exception {
    final IParameterProvider requestParams = getRequestParameters();
    final RepositoryFile prptFile = resolvePrptFile( requestParams );
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
                                   final AuditWrapper audit )
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
    if ( path.endsWith( PRPTI ) ) {
      reportComponent.setForceUnlockPreferredOutput( true );
    }
    reportComponent.setInputs( inputs );

    final AsyncJobFileStagingHandler handler = new AsyncJobFileStagingHandler( userSession );
    // will write to async stage target
    reportComponent.setOutputStream( handler.getStagingOutputStream() );

    final PentahoAsyncReportExecution
      asyncExec = new PentahoAsyncReportExecution( path, reportComponent, handler, userSession, instanceId, audit );

    final IPentahoAsyncExecutor executor =
      PentahoSystem.get( PentahoAsyncExecutor.class, PentahoAsyncExecutor.BEAN_NAME, null );
    // delegation
    if ( reportComponent.validate() ) {
      final UUID reservedId = getReservedId();

      final UUID uuid = executor.addTask( asyncExec, userSession, reservedId );
      sendSuccessRedirect( uuid );
    } else {
      // register failed parameters execution attempt
      audit.audit( userSession.getId(), userSession.getName(), path, getObjectName(), getClass().getName(),
        MessageTypes.FAILED, instanceId, "", 0, this );
      sendErrorResponse();
    }
  }

  private UUID getReservedId() throws IllegalStateException {

    final IJobIdGenerator iJobIdGenerator = PentahoSystem.get( IJobIdGenerator.class );
    final UUID failover = UUID.randomUUID();
    final String precomputedId =
      getRequestParameters().getStringParameter( RESERVED_ID, failover.toString() );

    final UUID uuid;
    try {
      uuid = UUID.fromString( precomputedId );
    } catch ( final IllegalArgumentException e ) {
      logger.warn( "Wrong uuid came from client side: ", e );
      return failover;
    }

    if ( iJobIdGenerator != null && iJobIdGenerator.acquire( userSession, uuid ) ) {
      return uuid;
    }

    return failover;
  }

  protected void sendErrorResponse() throws IOException {
    final HttpServletResponse httpResponse = getServletResponse();
    httpResponse.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
  }

  protected void sendSuccessRedirect( final UUID uuid ) throws IOException {
    final HttpServletResponse httpResponse = getServletResponse();
    final HttpServletRequest servletRequest = getServletRequest();
    final String contextUrl = servletRequest.getContextPath();
    httpResponse.setStatus( HttpServletResponse102.SC_PROCESSING );
    httpResponse.sendRedirect( contextUrl + REDIRECT_PREFIX + uuid.toString() + REDIRECT_POSTFIX );
  }

  protected HttpServletResponse getServletResponse() {
    final IParameterProvider pathProviders = parameterProviders.get( PATH );
    final Object httpResponseObj = pathProviders.getParameter( HTTPRESPONSE );
    return HttpServletResponse.class.cast( httpResponseObj );
  }

  protected HttpServletRequest getServletRequest() {
    final IParameterProvider pathProviders = parameterProviders.get( PATH );
    final Object httpRequestObj = pathProviders.getParameter( HTTPREQUEST );
    return HttpServletRequest.class.cast( httpRequestObj );
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

