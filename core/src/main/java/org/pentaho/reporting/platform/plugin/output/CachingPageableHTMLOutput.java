/*
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
 * Copyright (c) 2002-2019 Hitachi Vantara.  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin.output;

import mondrian.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.PerformanceTags;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportParameterValidationException;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.engine.classic.core.layout.output.DisplayAllFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.PageableHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.states.PerformanceMonitorContext;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.libraries.base.util.FormattedMessage;
import org.pentaho.reporting.libraries.base.util.PerformanceLoggingStopWatch;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.Repository;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceLoadingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.xmlns.parser.Base64;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachingPageableHTMLOutput extends PageableHTMLOutput {

  private static Log logger = LogFactory.getLog( CachingPageableHTMLOutput.class );
  public static final String IS_QUERY_LIMIT_REACHED = "IsQueryLimitReached";
  public static final String REPORT_ROWS = "ReportRows";
  private PageableReportProcessor processor;
  private String jcrOutputPath;

  private class CacheListener implements ReportProgressListener {

    private CacheListener( final String key, final int acceptedPage,
                   final PageableReportProcessor proc,
                   final Repository targetRepository,
                   final IAsyncReportListener asyncReportListener ) {
      this.key = key;
      this.acceptedPage = acceptedPage;
      this.proc = proc;
      this.targetRepository = targetRepository;
      this.asyncReportListener = asyncReportListener;
    }

    private final String key;
    private final int acceptedPage;
    private final PageableReportProcessor proc;
    private final Repository targetRepository;
    private final IAsyncReportListener asyncReportListener;

    private int lastAcceptedPageWritten;

    @Override public void reportProcessingStarted( final ReportProgressEvent reportProgressEvent ) {
      //ignore
    }

    @Override public void reportProcessingUpdate( final ReportProgressEvent reportProgressEvent ) {
      //When we request X page the index that comes to server is X-1
      //then to be sure that X page is already stored the current index in event should be X+1
      //in other words page + 2
      final int page;

      if ( asyncReportListener.getRequestedPage() > 0 ) {
        //user requested new page before original generation finished
        //Generation could already pass the page but we need to update cache anyway
        page = Math.max( asyncReportListener.getRequestedPage() + 2, reportProgressEvent.getPage() );
      } else {
        //it's just an original request
        page = acceptedPage + 2;
      }

      asyncReportListener.setIsQueryLimitReached( proc.isQueryLimitReached() );
      final boolean needToStorePages = reportProgressEvent.getPage() == page && reportProgressEvent.getPage() > lastAcceptedPageWritten;
      if ( reportProgressEvent.getActivity() == ReportProgressEvent.GENERATING_CONTENT
        && needToStorePages ) {
        // we finished pagination, and thus have the page numbers ready.
        // we also have pages in repository
        try {
          persistContent( key, produceReportContent( proc, targetRepository ), reportProgressEvent.getMaximumRow() );
          lastAcceptedPageWritten = page;
          //Update after pages are in cache
          asyncReportListener.updateGenerationStatus( page - 1 );
          asyncReportListener.setStatus( AsyncExecutionStatus.CONTENT_AVAILABLE );
        } catch ( final Exception e ) {
          logger.error( "Can't persist" );
        }
      }
    }

    @Override public void reportProcessingFinished( final ReportProgressEvent reportProgressEvent ) {
      //ignore
    }
  }

  @Override
  public int paginate( final MasterReport report, final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    try {
      String key = report.getContentCacheKey();
      if ( key == null ) {
        key = createKey( report );
      }
      final IReportContent cachedContent = getCachedContent( key );
      if ( cachedContent != null ) {
        return cachedContent.getPageCount();
      }

      final IReportContent freshCache = regenerateCache( report, yieldRate, key, 0 );
      return freshCache.getPageCount();
    } catch ( final CacheKeyException e ) {
      return super.paginate( report, yieldRate );
    }
  }

  @Override
  public synchronized int generate( final MasterReport report, final int acceptedPage,
                                    final OutputStream outputStream, final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    PerformanceLoggingStopWatch sw = null;

    if ( acceptedPage < 0 ) {
      return generateNonCaching( report, acceptedPage, outputStream, yieldRate );
    }

    try {
      String key = report.getContentCacheKey();
      if ( key == null ) {
        key = createKey( report );
      }

      final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
      final IReportContent cachedContent = getCachedContent( key );
      final boolean forcePaginated = isForceAllPages( report );
      if ( cachedContent == null || ( listener != null && listener.isScheduled() && cachedContent.getPageCount() != cachedContent.getStoredPageCount() ) ) {
        logger.warn( "No cached content found for key: " + key );
        final IReportContent freshCache = regenerateCache( report, yieldRate, key, acceptedPage );

        if ( freshCache != null ) {
          //write all pages for scheduling case
          if ( forcePaginated || ( listener != null && listener.isScheduled() ) ) {
            PaginationControlWrapper.write( outputStream, freshCache );
            return freshCache.getPageCount();
          }
          setQueryLimitReachedToListener( key, listener );

          final byte[] pageData = freshCache.getPageData( acceptedPage );

          outputStream.write( pageData );
          outputStream.flush();
          return freshCache.getPageCount();
        } else {
          return -1;
        }
      }

      sw = ClassicEngineBoot.getInstance().getObjectFactory().get( PerformanceMonitorContext.class ).createStopWatch( PerformanceTags.REPORT_PARAMETER_QUERY,
              new FormattedMessage( "Serving Cached Content " ) );
      sw.start();

      setQueryLimitReachedToListener( key, listener );

      final byte[] page = cachedContent.getPageData( acceptedPage );
      if ( page != null && page.length > 0 ) {
        logger.warn( "Using cached report data for " + key );
        if ( listener != null ) {
          listener.updateGenerationStatus( cachedContent.getStoredPageCount() );
          final ReportProgressEvent event =
            new ReportProgressEvent( this, ReportProgressEvent.GENERATING_CONTENT, 0, getReportTotalRows( key ),
              acceptedPage + 1, cachedContent.getPageCount(), 0, 0 );
          listener.reportProcessingUpdate( event );
          listener.reportProcessingFinished( event );
        }

        //write all pages for scheduling case
        if ( forcePaginated || ( listener != null && listener.isScheduled() ) ) {
          PaginationControlWrapper.write( outputStream, cachedContent );
          return cachedContent.getPageCount();
        }
        outputStream.write( page );
        outputStream.flush();
        return cachedContent.getPageCount();
      }

      final IReportContent fullReportCache = regenerateCache( report, yieldRate, key, acceptedPage );

      //BACKLOG-8579
      if ( forcePaginated || ( listener != null && listener.isScheduled() ) ) {
        PaginationControlWrapper.write( outputStream, fullReportCache );
        return fullReportCache.getPageCount();
      }

      outputStream.write( fullReportCache.getPageData( acceptedPage ) );
      outputStream.flush();
      return fullReportCache.getPageCount();
    } catch ( final CacheKeyException e ) {
      return generateNonCaching( report, acceptedPage, outputStream, yieldRate );
    } finally {
      if ( sw != null ) {
        sw.close();
      }
    }
  }

  protected boolean isJcrImagesAndCss() {
    return getJcrOutputPath() != null && !getJcrOutputPath().isEmpty();
  }

  @Override
  protected Pair<ContentLocation, PentahoNameGenerator> reinitLocationAndNameGenerator()
          throws ReportProcessingException, ContentIOException {

    if ( !isJcrImagesAndCss() ) {
      return super.reinitLocationAndNameGenerator();
    } else {
      IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
      final RepositoryFile outputFolder = repo.getFile( jcrOutputPath );

      final ReportContentRepository repository = new ReportContentRepository( outputFolder );
      final ContentLocation dataLocation = repository.getRoot();

      final PentahoNameGenerator dataNameGenerator = createPentahoNameGenerator();
      dataNameGenerator.initialize( dataLocation, isSafeToDelete() );

      return Pair.of( dataLocation, dataNameGenerator );
    }
  }

  @Override
  protected boolean shouldUseContentIdAsName() {
    return isJcrImagesAndCss();
  }

  private void setQueryLimitReachedToListener( String key, IAsyncReportListener listener ) {
    Map<String, Serializable> metaData = getCachedMetaData( key );
    if ( metaData != null && listener != null ) {
      Object isQueryLimitReached = metaData.get( IS_QUERY_LIMIT_REACHED );
      if ( isQueryLimitReached != null ) {
        listener.setIsQueryLimitReached( (boolean) isQueryLimitReached );
      }
    }
  }

  IReportContent regenerateCache( final MasterReport report, final int yieldRate, final String key,
                                  final int acceptedPage )
    throws ReportProcessingException {
    logger.warn( "Regenerating report data for " + key );
    final IReportContent result = produceCacheablePages( report, yieldRate, key, acceptedPage );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    if ( listener != null ) {
      persistContent( key, result, listener.getTotalRows() );
    } else {
      persistContent( key, result, report.getQueryLimit() );
    }

    return result;
  }

  private IReportContent produceCacheablePages( final MasterReport report, final int yieldRate, final String key,
                                                final int acceptedPage )
    throws ReportProcessingException {

    final PageableReportProcessor proc = createReportProcessor( report, yieldRate );
    processor = proc;

    final PageableHtmlOutputProcessor outputProcessor = (PageableHtmlOutputProcessor) proc.getOutputProcessor();
    outputProcessor.setFlowSelector( new DisplayAllFlowSelector() );

    //Async listener
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    ReportProgressListener cacheListener = null;
    try {
      final Repository targetRepository = reinitOutputTargetRepo();
      if ( listener != null ) {
        if ( listener.isFirstPageMode() ) {
          //Create cache listener to write first requested page when needed
          cacheListener = new CacheListener( key, acceptedPage, proc, targetRepository, listener );
          proc.addReportProgressListener( cacheListener );
        }
        proc.addReportProgressListener( listener );
      }
      proc.processReport();
      return produceReportContent( proc, targetRepository );
    } catch ( final ContentIOException | IOException | ReportDataFactoryException | ReportParameterValidationException e ) {
      if ( e.getMessage() != null && listener != null ) {
        listener.setErrorMessage( e.getMessage() );
      }
      return null;
    } finally {
      if ( listener != null ) {
        proc.removeReportProgressListener( listener );
      }
      if ( cacheListener != null ) {
        proc.removeReportProgressListener( cacheListener );
      }
    }
  }


  int generateNonCaching( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                                  final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    return super.generate( report, acceptedPage, outputStream, yieldRate );
  }


  public IReportContent getCachedContent( final String key ) {
    final IPluginCacheManager cacheManager = PentahoSystem.get( IPluginCacheManager.class );
    final IReportContentCache cache = cacheManager.getCache();
    return cache.get( key );
  }

  private Map<String, Serializable> getCachedMetaData( final String key ) {
    final IPluginCacheManager cacheManager = PentahoSystem.get( IPluginCacheManager.class );
    final IReportContentCache cache = cacheManager.getCache();
    return cache.getMetaData( key );
  }

  private synchronized void persistContent( final String key, final IReportContent data, final int reportTotalRows ) {
    final IPluginCacheManager cacheManager = PentahoSystem.get( IPluginCacheManager.class );
    final IReportContentCache cache = cacheManager.getCache();
    if ( cache != null ) {
      Map<String, Serializable> metaData = cache.getMetaData( key );
      if ( metaData == null ) {
        metaData = new HashMap<>();
      }
      metaData.put( REPORT_ROWS, reportTotalRows );

      if ( processor.isQueryLimitReached() ) {
        updateQueryLimitReachedFlag( metaData );
      }

      cache.put( key, data, metaData );
    } else {
      logger.error( "Plugin session cache is not available." );
    }
  }

  private Map<String, Serializable> updateQueryLimitReachedFlag( Map<String, Serializable> metaData ) {
    if ( metaData == null ) {
      metaData = new HashMap<>();
    }
    metaData.put( IS_QUERY_LIMIT_REACHED, processor.isQueryLimitReached() );

    return metaData;
  }

  private Serializable computeCacheKey( final MasterReport report ) throws BeanException {
    final ResourceKey definitionSource = report.getDefinitionSource();

    //We need a parent because ZipRepository always has the same values
    final ResourceKey parent = definitionSource.getParent();
    final List<String> sourceKey;
    if ( parent != null ) {
      sourceKey = computeDefSourceKey( report, parent );
    } else {
      sourceKey = computeDefSourceKey( report, definitionSource );
    }
    final HashMap<String, String> params = new HashMap<>();
    params.put( "query-limit", String.valueOf( report.getQueryLimit() ) );
    final ReportParameterDefinition parameterDefinition = report.getParameterDefinition();
    final ReportParameterValues parameterValues = report.getParameterValues();
    for ( final ParameterDefinitionEntry p : parameterDefinition.getParameterDefinitions() ) {
      final String name = p.getName();
      final Object o = parameterValues.get( name );
      if ( o == null ) {
        params.put( name, null );
      } else {
        params.put( name, ConverterRegistry.toAttributeValue( o ) );
      }
    }
    final ArrayList<Object> key = new ArrayList<>();
    key.add( sourceKey );
    key.add( params );
    key.add( report.getReportEnvironment().getLocale().toString() );
    return key;
  }

  private List<String> computeDefSourceKey( final MasterReport report, final ResourceKey definitionSource ) {
    final ArrayList<String> sourceKey = new ArrayList<>();
    if ( definitionSource.getIdentifierAsString() != null ) {
      sourceKey.add( String.valueOf( definitionSource.getSchema() ) );
      sourceKey.add( definitionSource.getIdentifierAsString() );
    }
    //Check if report was replaced in repository
    final String rawDataVersion = getRawDataVersion( report, definitionSource );
    if ( null != rawDataVersion ) {
      sourceKey.add( rawDataVersion );
    }
    return sourceKey;
  }

  private String getRawDataVersion( final MasterReport report, final ResourceKey definitionSource ) {
    String result = null;
    ResourceManager resourceManager = report.getResourceManager();
    if ( resourceManager == null ) {
      resourceManager = new ResourceManager();
    }
    final ResourceData resourceData;
    try {
      resourceData = resourceManager.loadRawData( definitionSource );
      final long version = resourceData.getVersion( resourceManager );
      if ( version != -1 ) {
        result = String.valueOf( version );
      }
    } catch ( final ResourceLoadingException e ) {
      logger.warn( "Can't load resource data for cache key computation: ", e );
    }
    return result;
  }

  private byte[] keyToBytes( final Serializable s ) throws IOException {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final ObjectOutputStream oout = new ObjectOutputStream( bout );
    oout.writeObject( s );
    oout.close();
    bout.toByteArray();
    return bout.toByteArray();
  }

  // will be 43 characters long. Good enough for a directory name, even on Windows.
  public String createKey( final MasterReport report )
    throws CacheKeyException {
    try {
      final Serializable keyRaw = computeCacheKey( report );
      final byte[] text = keyToBytes( keyRaw );
      final MessageDigest md = MessageDigest.getInstance( "SHA-256" );
      md.update( text ); // Change this to "UTF-16" if needed
      final byte[] digest = md.digest();
      return new String( Base64.encode( digest ) );
    } catch ( final Exception b ) {
      throw new CacheKeyException( b );
    }
  }

  private static class CacheKeyException extends Exception {
    private CacheKeyException( final Throwable cause ) {
      super( cause );
    }
  }

  private int getReportTotalRows( final String key ) {
    final Map<String, Serializable> metaData = getCachedMetaData( key );
    if ( metaData != null ) {
      final Object reportTotalRows = metaData.get( REPORT_ROWS );
      if ( reportTotalRows != null ) {
        return (int) reportTotalRows;
      }
    }
    return -1;
  }

  public String getJcrOutputPath() {
    return jcrOutputPath;
  }

  public void setJcrOutputPath( String jcrOutputPath ) {
    this.jcrOutputPath = jcrOutputPath;
  }
}
