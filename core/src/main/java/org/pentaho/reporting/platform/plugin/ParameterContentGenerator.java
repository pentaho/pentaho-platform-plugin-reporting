/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


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
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;

public class ParameterContentGenerator extends SimpleContentGenerator {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String path = null;

  @Override
  public void createContent( final OutputStream outputStream ) throws Exception {
    // set instance Id if debug is enabled.
    ReportListenerThreadHolder.setRequestId( this.instanceId );

    final IParameterProvider requestParams = getRequestParameters();

    final RepositoryFile prptFile = resolvePrptFile( requestParams );

    final RenderType renderMode =
        RenderType
            .valueOf( requestParams.getStringParameter( "renderMode", RenderType.XML.toString() ).toUpperCase() ); //$NON-NLS-1$

    switch ( renderMode ) {
      case XML: {
        final ParameterXmlContentHandler parameterXmlContentHandler = getParameterXmlContentHandler( this, true );
        parameterXmlContentHandler.createParameterContent( outputStream, prptFile.getId(), prptFile.getPath(), false,
            null );
        break;
      }
      case PARAMETER: {
        final ParameterXmlContentHandler parameterXmlContentHandler = getParameterXmlContentHandler( this, false );
        parameterXmlContentHandler.createParameterContent( outputStream, prptFile.getId(), prptFile.getPath(), false,
            null );
        break;
      }
      default:
        throw new IllegalArgumentException();
    }
  }

  protected ParameterXmlContentHandler getParameterXmlContentHandler( ParameterContentGenerator contentGenerator, boolean paginate ) {
    return new ParameterXmlContentHandler( contentGenerator, paginate );
  }

  protected RepositoryFile resolvePrptFile( final IParameterProvider requestParams ) throws UnsupportedEncodingException {
    final IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );
    final IParameterProvider pathParams = getPathParameters();

    if ( requestParams != null && requestParams.getStringParameter( "path", null ) != null ) {
      path = requestParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    } else if ( pathParams != null && pathParams.getStringParameter( "path", null ) != null ) {
      path = pathParams.getStringParameter( "path", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    path = idTopath( URLDecoder.decode( path, "UTF-8" ) );

    return unifiedRepository.getFile( path );
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

    return parameterProviders.get( IParameterProvider.SCOPE_REQUEST );
  }

  public IParameterProvider getPathParameters() {
    return parameterProviders.get( "path" );
  }

  public Map<String, Object> createInputs() {
    return createInputs( getRequestParameters() );
  }

  protected static Map<String, Object> createInputs( final IParameterProvider requestParams ) {
    final Map<String, Object> inputs = new HashMap<>();
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
