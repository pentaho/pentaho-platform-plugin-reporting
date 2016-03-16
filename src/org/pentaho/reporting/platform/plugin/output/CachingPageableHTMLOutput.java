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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.engine.classic.core.layout.output.DisplayAllFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.PageableHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.PageSequenceNameGenerator;
import org.pentaho.reporting.libraries.repository.file.FileRepository;
import org.pentaho.reporting.libraries.repository.zip.ZipRepository;
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
import org.pentaho.reporting.platform.plugin.cache.ReportContentImpl;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

  private class CacheListener implements ReportProgressListener {

    public CacheListener( final String key, final int acceptedPage,
                          final PageableReportProcessor proc,
                          final ZipRepository targetRepository,
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
    private final ZipRepository targetRepository;
    private final IAsyncReportListener asyncReportListener;

    @Override public void reportProcessingStarted( final ReportProgressEvent reportProgressEvent ) {
      //ignore
    }

    @Override public void reportProcessingUpdate( final ReportProgressEvent reportProgressEvent ) {
      //When we request X page the accepted-page is X-1
      //then to be sure that X page is already stored the current page in event should be X+1
      //in other words acceptedPage + 2
      final boolean requestedPageIsStored = reportProgressEvent.getPage() == acceptedPage + 2;
      if ( reportProgressEvent.getActivity() == ReportProgressEvent.GENERATING_CONTENT
        && requestedPageIsStored ) {
        // we finished pagination, and thus have the page numbers ready.
        // we also have pages in repository
        try {
          persistContent( key, produceReportContent( proc, targetRepository ) );
        } catch ( final Exception e ) {
          logger.error( "Can't persist" );
        }
        //Update after pages are in cache
        asyncReportListener.setTotalPages( proc.getLogicalPageCount() );
        asyncReportListener.setStatus( AsyncExecutionStatus.CONTENT_AVAILABLE );
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
  public synchronized int generate( final MasterReport report, /*final*/ int acceptedPage,
                                    final OutputStream outputStream, final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {

    if ( acceptedPage < 0 ) {
     /* return generateNonCaching( report, acceptedPage, outputStream, yieldRate );*/
      acceptedPage = 0;
    }

    try {
      String key = report.getContentCacheKey();
      if ( key == null ) {
        key = createKey( report );
      }

      final IReportContent cachedContent = getCachedContent( key );

      if ( cachedContent == null ) {
        logger.warn( "No cached content found for key: " + key );
        final IReportContent freshCache = regenerateCache( report, yieldRate, key, acceptedPage );
        final byte[] pageData = freshCache.getPageData( acceptedPage );
        outputStream.write( pageData );
        outputStream.flush();
        return freshCache.getPageCount();
      }

      final byte[] page = cachedContent.getPageData( acceptedPage );
      if ( page != null && page.length > 0 ) {
        logger.warn( "Using cached report data for " + key );
        final ReportProgressListener listener = ReportListenerThreadHolder.getListener();
        if ( listener != null ) {
          listener.reportProcessingFinished(
            new ReportProgressEvent( "Cache", ReportProgressEvent.GENERATING_CONTENT, 0, 0,
              cachedContent.getPageCount(), 0, 0 ) );
        }
        outputStream.write( page );
        outputStream.flush();
        return cachedContent.getPageCount();
      }


      final IReportContent data = regenerateCache( report, yieldRate, key, acceptedPage );
      outputStream.write( data.getPageData( acceptedPage ) );
      outputStream.flush();
      return data.getPageCount();
    } catch ( final CacheKeyException e ) {
      return generateNonCaching( report, acceptedPage, outputStream, yieldRate );
    }
  }

  private int extractPageFromName( final String name ) {
    if ( name.startsWith( "page-" ) && name.endsWith( ".html" ) ) {
      try {
        final String number = name.substring( "page-".length(), name.length() - ".html".length() );
        return Integer.parseInt( number );
      } catch ( final Exception e ) {
        // ignore invalid names. Outside of a PoC it is probably a good idea to check errors properly.
        return -1;
      }
    }
    return -1;
  }

  private IReportContent regenerateCache( final MasterReport report, final int yieldRate, final String key,
                                          final int acceptedPage )
    throws ReportProcessingException {
    logger.warn( "Regenerating report data for " + key );
    final IReportContent result = produceCacheablePages( report, yieldRate, key, acceptedPage );
    persistContent( key, result );
    return result;
  }

  private IReportContent produceCacheablePages( final MasterReport report, final int yieldRate, final String key,
                                                final int acceptedPage )
    throws ReportProcessingException {

    final PageableReportProcessor proc = createReportProcessor( report, yieldRate );

    final PageableHtmlOutputProcessor outputProcessor = (PageableHtmlOutputProcessor) proc.getOutputProcessor();
    outputProcessor.setFlowSelector( new DisplayAllFlowSelector() );

    //Async listener
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    ReportProgressListener cacheListener = null;
    try {
      final ZipRepository targetRepository = reinitOutputTargetForCaching();
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
    } catch ( final ContentIOException e ) {
      return null;
    } catch ( final IOException e ) {
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


  private IReportContent produceReportContent( final PageableReportProcessor proc,
                                               final ZipRepository targetRepository )
    throws ContentIOException, IOException {
    final int pageCount = proc.getPhysicalPageCount();
    final ContentLocation root = targetRepository.getRoot();
    final Map<Integer, byte[]> pages = new HashMap<>();

    for ( final ContentEntity contentEntities : root.listContents() ) {
      if ( contentEntities instanceof ContentItem ) {
        final ContentItem ci = (ContentItem) contentEntities;
        final String name = ci.getName();
        final int pageNumber = extractPageFromName( name );
        if ( pageNumber >= 0 ) {
          pages.put( pageNumber, read( ci.getInputStream() ) );
        }
      }
    }
    return new ReportContentImpl( pageCount, pages );
  }

  private byte[] read( final InputStream in ) throws IOException {
    try {
      return IOUtils.toByteArray( in );
    } finally {
      in.close();
    }
  }

  protected ZipRepository reinitOutputTargetForCaching() throws ReportProcessingException, ContentIOException {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();

    final ContentLocation dataLocation;
    final PentahoNameGenerator dataNameGenerator;
    if ( ctx != null ) {
      File dataDirectory = new File( ctx.getFileOutputPath( "system/tmp/" ) ); //$NON-NLS-1$
      if ( dataDirectory.exists() && ( dataDirectory.isDirectory() == false ) ) {
        dataDirectory = dataDirectory.getParentFile();
        if ( dataDirectory.isDirectory() == false ) {
          throw new ReportProcessingException( "Dead " + dataDirectory.getPath() ); //$NON-NLS-1$
        }
      } else if ( dataDirectory.exists() == false ) {
        dataDirectory.mkdirs();
      }

      final FileRepository dataRepository = new FileRepository( dataDirectory );
      dataLocation = dataRepository.getRoot();
      dataNameGenerator = PentahoSystem.get( PentahoNameGenerator.class );
      if ( dataNameGenerator == null ) {
        throw new IllegalStateException( Messages.getInstance().getString(
          "ReportPlugin.errorNameGeneratorMissingConfiguration" ) );
      }
      dataNameGenerator.initialize( dataLocation, true );
    } else {
      dataLocation = null;
      dataNameGenerator = null;
    }

    final ZipRepository targetRepository = new ZipRepository(); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    final HtmlPrinter printer = getPrinter();
    // generates predictable file names like "page-0.html", "page-1.html" and so on.
    printer.setContentWriter( targetRoot,
      new PageSequenceNameGenerator( targetRoot, "page", "html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    printer.setDataWriter( dataLocation, dataNameGenerator );

    return targetRepository;
  }

  private int generateNonCaching( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                                  final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    return super.generate( report, acceptedPage, outputStream, yieldRate );
  }


  public IReportContent getCachedContent( final String key ) {
    final IPluginCacheManager cacheManager = PentahoSystem.get( IPluginCacheManager.class );
    final IReportContentCache cache = cacheManager.getCache();
    return cache.get( key );
  }

  private synchronized void persistContent( final String key, final IReportContent data ) {
    final IPluginCacheManager cacheManager = PentahoSystem.get( IPluginCacheManager.class );
    final IReportContentCache cache = cacheManager.getCache();
    if ( cache != null ) {
      cache.put( key, data );
    } else {
      logger.error( "Plugin session cache is not available." );
    }
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
}
