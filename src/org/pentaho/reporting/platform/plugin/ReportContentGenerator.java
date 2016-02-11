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

import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.pluginmgr.PluginClassLoader;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;

public class ReportContentGenerator extends ParameterContentGenerator {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public enum RENDER_TYPE {
    REPORT, XML, PARAMETER, DOWNLOAD, ASYNC
  }

  private static final Log log = LogFactory.getLog( ReportContentGenerator.class );

  public ReportContentGenerator() {
  }

  @Override
  public void createContent( final OutputStream outputStream ) throws Exception {
    final String id = UUIDUtil.getUUIDAsString();
    String path = null;
    RENDER_TYPE renderMode = null;
    setInstanceId( id );
    IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );
    final IParameterProvider requestParams = getRequestParameters();
    final IParameterProvider pathParams = getPathParameters();
    
    if ( requestParams != null && requestParams.getStringParameter( "path", null ) != null ) {
      path = requestParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    } else if ( pathParams != null && pathParams.getStringParameter( "path", null ) != null ) {
      path = pathParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    }
    path = URLDecoder.decode( path, "UTF-8");

    if ( requestParams != null && requestParams.getStringParameter( "renderMode", null ) != null ) {
      renderMode =
          RENDER_TYPE.valueOf( requestParams
              .getStringParameter( "renderMode", RENDER_TYPE.REPORT.toString() ).toUpperCase() ); //$NON-NLS-1$
    } else if ( pathParams != null && pathParams.getStringParameter( "renderMode", null ) != null ) {
      renderMode =
          RENDER_TYPE.valueOf( pathParams
              .getStringParameter( "renderMode", RENDER_TYPE.REPORT.toString() ).toUpperCase() ); //$NON-NLS-1$
    }

    // If render mode is not passed in the request or path parameter, then we will assume that the render type is REPORT
    if ( renderMode == null ) {
      renderMode = RENDER_TYPE.REPORT;
    }

    RepositoryFile prptFile = unifiedRepository.getFile( path );

    try {
      switch ( renderMode ) {
        case DOWNLOAD: {
          final DownloadReportContentHandler contentHandler =
              new DownloadReportContentHandler( userSession, parameterProviders.get( "path" ) ); //$NON-NLS-1$
          contentHandler.createDownloadContent( outputStream, idTopath( prptFile.getPath() ) );
          break;
        }
        case REPORT: {
          // create inputs from request parameters
          final ExecuteReportContentHandler executeReportContentHandler =
             new ExecuteReportContentHandler( this ); //$NON-NLS-1$
          executeReportContentHandler.createReportContent( outputStream, prptFile.getId(), prptFile.getPath(), false );
          break;
        }
        default:
          throw new IllegalArgumentException();
      }
    } catch ( Exception ex ) {
      final String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
      log.error( exceptionMessage, ex );

      if ( outputStream != null ) {
        outputStream.write( exceptionMessage.getBytes( "UTF-8" ) ); //$NON-NLS-1$
        outputStream.flush();
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  public String getInstanceId() {
    return instanceId;
  }

  public Map<String, IParameterProvider> getParameterProviders() {
    return parameterProviders;
  }

  public Log getLogger() {
    return log;
  }

  public String getMimeType() {
    final IParameterProvider requestParams = getRequestParameters();
    final IParameterProvider pathParams = getPathParameters();
    RENDER_TYPE renderMode = null;
    String path = null;
    IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );
    if ( requestParams != null && requestParams.getStringParameter( "renderMode", null ) != null ) {
      renderMode =
          RENDER_TYPE.valueOf( requestParams
              .getStringParameter( "renderMode", RENDER_TYPE.REPORT.toString() ).toUpperCase() ); //$NON-NLS-1$
    } else if ( pathParams != null && pathParams.getStringParameter( "renderMode", null ) != null ) {
      renderMode =
          RENDER_TYPE.valueOf( pathParams
              .getStringParameter( "renderMode", RENDER_TYPE.REPORT.toString() ).toUpperCase() ); //$NON-NLS-1$
    }
    // If render mode is not passed in the request or path parameter, then we will assume that the render type is REPORT
    if ( renderMode == null ) {
      renderMode = RENDER_TYPE.REPORT;
    }
    if ( renderMode.equals( RENDER_TYPE.XML ) || renderMode.equals( RENDER_TYPE.PARAMETER ) ) {
      return "text/xml"; //$NON-NLS-1$
    } else if ( renderMode.equals( RENDER_TYPE.DOWNLOAD ) ) {
      // perhaps we can invent our own mime-type or use application/zip?
      return "application/octet-stream"; //$NON-NLS-1$
    }
    try {
      if ( requestParams != null && requestParams.getStringParameter( "path", null ) != null ) {
        path = requestParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
      } else if ( pathParams != null && pathParams.getStringParameter( "path", null ) != null ) {
        path = pathParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
      }
      path = idTopath( URLDecoder.decode(path, "UTF-8") );
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    RepositoryFile prptFile = unifiedRepository.getFile( path );
    final boolean isMobile = "true".equals( requestParams.getStringParameter( "mobile", "false" ) ); //$NON-NLS-1$ //$NON-NLS-2$

    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    final Map<String, Object> inputs = createInputs( requestParams );
    reportComponent.setForceDefaultOutputTarget( isMobile );
    if ( prptFile.getPath().endsWith( ".prpti" ) ) {
      reportComponent.setForceUnlockPreferredOutput( true );
    }
    reportComponent.setDefaultOutputTarget( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE );
    reportComponent.setReportFileId( prptFile.getId() );
    reportComponent.setInputs( inputs );
    return reportComponent.getMimeType();
  }

  public String getSystemRelativePluginPath( ClassLoader classLoader ) {
    File dir = getPluginDir( classLoader );
    if ( dir == null ) {
      return null;
    }
    // get the full path with \ converted to /
    String path = dir.getAbsolutePath().replace( "\\", RepositoryFile.SEPARATOR );
    int pos = path.lastIndexOf( RepositoryFile.SEPARATOR + "system" + RepositoryFile.SEPARATOR ); //$NON-NLS-1$
    if ( pos != -1 ) {
      path = path.substring( pos + 8 );
    }
    return path;
  }

  protected File getPluginDir( ClassLoader classLoader ) {
    if ( classLoader instanceof PluginClassLoader ) {
      return ( (PluginClassLoader) classLoader ).getPluginDir();
    }
    return null;
  }

}
