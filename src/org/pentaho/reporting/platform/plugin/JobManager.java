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
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by dima.prokopenko@gmail.com on 2/8/2016.
 */
@Path("/jobs")
public class JobManager {

  private static final Log logger = LogFactory.getLog( JobManager.class );

  public enum Status {
    QUEUED, WORKING, CONTENT_AVAILABLE, FINISHED
  }

  public class PutReportJson {
    String path;
    UUID uuid;
    Status status;
    byte progress = 0;
  }

  @GET
  @Path("{job_id}/content")
  public Object getContent(@PathParam("job_id") String job_id) {


    return new Object();
  }

  @GET
  @Path("{job_id}/status")
  @Produces("application/json")
  public String getStatus(@PathParam("job_id") String job_id) {
    UUID uuid = null;
    try {
      UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Invalid UUID: " + job_id + "  " );
    }




    ObjectMapper mapper = new ObjectMapper();
    PutReportJson response = new PutReportJson();

    try {
      return mapper.writeValueAsString( response );
    } catch ( IOException e ) {
      //TODO log
      return "{error}";
    }
  }
}
