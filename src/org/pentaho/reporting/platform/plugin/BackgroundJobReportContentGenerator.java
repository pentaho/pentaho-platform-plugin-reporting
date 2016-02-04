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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.cache.PentahoAsyncCache;
import org.pentaho.reporting.platform.plugin.cache.PentahoAsyncReportExecution;
import org.pentaho.reporting.platform.plugin.staging.StagingHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * push future task and send back redirect.
 *
 * Created by dima.prokopenko@gmail.com on 2/4/2016.
 */
public class BackgroundJobReportContentGenerator {

  private ReportContentGenerator contentGenerator;
  private IPentahoSession userSession;

  private static final Log logger = LogFactory.getLog( BackgroundJobReportContentGenerator.class );

  public BackgroundJobReportContentGenerator( final ReportContentGenerator contentGenerator ) {
    this.contentGenerator = contentGenerator;
    this.userSession = contentGenerator.getUserSession();
  }

  public void createReportContent( final OutputStream outputStream, final Serializable fileId, final String path ) throws Exception {
    final Map<String, Object> inputs = contentGenerator.createInputs();

    //TODO correct audit record to mark async?
    AuditHelper.audit( userSession.getId(), userSession.getName(), path, contentGenerator.getObjectName(), getClass()
        .getName(), MessageTypes.INSTANCE_START, contentGenerator.getInstanceId(), "", 0, contentGenerator ); //$NON-NLS-1$

    // prepare execution, copy-paste from ExecuteReportContentHandler.
    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    reportComponent.setReportFileId( fileId );
    reportComponent.setPaginateOutput( true );
    reportComponent.setForceDefaultOutputTarget( false );
    reportComponent.setDefaultOutputTarget( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE );
    if ( path.endsWith( ".prpti" ) ) {
      reportComponent.setForceUnlockPreferredOutput( true );
    }
    reportComponent.setInputs( inputs );

    //TODO stub for async staging handler
    StagingHandler handler = new StagingHandler(){
      @Override public StagingMode getStagingMode() {
        return null;
      }
      @Override public boolean isFullyBuffered() {
        return false;
      }

      @Override public boolean canSendHeaders() {
        return false;
      }

      @Override public OutputStream getStagingOutputStream() {
        return null;
      }

      @Override public void complete() throws IOException {

      }

      @Override public void close() {

      }

      @Override public int getWrittenByteCount() {
        return 0;
      }
    };

    /*
    * This creates the background job / future and invokes it.
    * */
    PentahoAsyncReportExecution asyncExec = new PentahoAsyncReportExecution( reportComponent, handler );
    PentahoAsyncCache executor = PentahoSystem.get( PentahoAsyncCache.class,  "pentaho-reporting-async-thread-pool", null );



    /*
    * The job/future has a UUID that identifies it in the system.
    * The request returns
    *
    * with either error (when parameters do not validate)
    * or
    * status code 102, along with a "Location" header pointing to the job API and the UUID for that job.
    * */


  }
}

