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

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;

public class ParameterContentGenerator extends SimpleContentGenerator {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String path = null;

  public enum RENDER_TYPE {
    REPORT, XML, PARAMETER, SUBSCRIBE, DOWNLOAD
  }

  @Override
  public void createContent( OutputStream outputStream ) throws Exception {
    final IParameterProvider requestParams = getRequestParameters();

    RepositoryFile prptFile = resolvePrptFile( requestParams );

    final RENDER_TYPE renderMode =
        RENDER_TYPE
            .valueOf( requestParams.getStringParameter( "renderMode", RENDER_TYPE.XML.toString() ).toUpperCase() ); //$NON-NLS-1$

    switch ( renderMode ) {
      case XML: {
        final ParameterXmlContentHandler parameterXmlContentHandler = new ParameterXmlContentHandler( this, true );
        parameterXmlContentHandler.createParameterContent( outputStream, prptFile.getId(), prptFile.getPath(), false,
            null );
        break;
      }
      case PARAMETER: {
        final ParameterXmlContentHandler parameterXmlContentHandler = new ParameterXmlContentHandler( this, false );
        parameterXmlContentHandler.createParameterContent( outputStream, prptFile.getId(), prptFile.getPath(), false,
            null );
        break;
      }
      default:
        throw new IllegalArgumentException();
    }
  }

  protected RepositoryFile resolvePrptFile( IParameterProvider requestParams ) throws UnsupportedEncodingException {
    IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );
    final IParameterProvider pathParams = getPathParameters();

    if ( requestParams != null && requestParams.getStringParameter( "path", null ) != null ) {
      path = requestParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    } else if ( pathParams != null && pathParams.getStringParameter( "path", null ) != null ) {
      path = pathParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    path = idTopath( URLDecoder.decode( path, "UTF-8" ) );

    RepositoryFile prptFile = unifiedRepository.getFile( path );

    return prptFile;
  }

  @Override
  public String getMimeType() {
    return "text/xml";
  }

  @Override
  public Log getLogger() {
    return LogFactory.getLog( ParameterContentGenerator.class );
  }

  protected String idTopath( String path ) {
    if ( path != null && path.length() > 0 && path.charAt( 0 ) != '/' ) {
      path = "/" + path;
    }
    return path;
  }

  /**
   * Safely get our request parameters
   * 
   * @return IParameterProvider the provider of parameters
   */
  public IParameterProvider getRequestParameters() {
    if ( parameterProviders == null ) {
      return new SimpleParameterProvider();
    }

    IParameterProvider requestParams = parameterProviders.get( IParameterProvider.SCOPE_REQUEST );
    return requestParams;
  }

  public IParameterProvider getPathParameters() {
    IParameterProvider pathParams = parameterProviders.get( "path" );
    return pathParams;
  }

  public Map<String, Object> createInputs() {
    return createInputs( getRequestParameters() );
  }

  protected static Map<String, Object> createInputs( final IParameterProvider requestParams ) {
    final Map<String, Object> inputs = new HashMap<String, Object>();
    if ( requestParams == null ) {
      return inputs;
    }

    final Iterator<?> paramIter = requestParams.getParameterNames();
    while ( paramIter.hasNext() ) {
      final String paramName = (String) paramIter.next();
      final Object paramValue = requestParams.getParameter( paramName );
      if ( paramValue == null ) {
        continue;
      }
      // only actually add inputs who don't have NULL values
      inputs.put( paramName, paramValue );
    }
    return inputs;
  }

  public IPentahoSession getUserSession() {
    return userSession;
  }
}
