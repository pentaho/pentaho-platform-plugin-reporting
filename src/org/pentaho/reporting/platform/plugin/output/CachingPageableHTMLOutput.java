package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
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
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.xmlns.parser.Base64;
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
import java.util.Map;

public class CachingPageableHTMLOutput extends PageableHTMLOutput {

  private static Log logger = LogFactory.getLog( CachingPageableHTMLOutput.class );

  public CachingPageableHTMLOutput() {
  }

  @Override
  public int paginate( final MasterReport report, final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    try {
      final String key = createKey( report );
      final Integer pageCountCached = getPageCount( key );
      if ( pageCountCached != null ) {
        return pageCountCached.intValue();
      }

      final int pageCount = super.paginate( report, yieldRate );
      putPageCount( key, pageCount );
      return pageCount;
    } catch ( final CacheKeyException e ) {
      return super.paginate( report, yieldRate );
    }
  }

  @Override
  public synchronized int generate( final MasterReport report, final int acceptedPage,
                                    final OutputStream outputStream, final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {

    final Object isReportCacheEnabled =
      report.getAttribute( AttributeNames.Pentaho.NAMESPACE, AttributeNames.Pentaho.DYNAMIC_REPORT_CACHE );

    if ( acceptedPage < 0 || Boolean.FALSE.equals( isReportCacheEnabled ) ) {
      return generateNonCaching( report, acceptedPage, outputStream, yieldRate );
    }

    try {
      final String key = createKey( report );
      final Integer pageCount = getPageCount( key );
      if ( pageCount == null ) {
        logger.warn( "No cached page count for " + key );
        final PageResult data = regenerateCache( report, acceptedPage, key, yieldRate );
        outputStream.write( data.data );
        outputStream.flush();
        return data.pageCount;
      }

      final PageResult pageData = getPage( key, acceptedPage );
      if ( pageData != null ) {
        logger.warn( "Using cached report data for " + key );
        outputStream.write( pageData.data );
        outputStream.flush();
        return pageData.pageCount;
      }


      final PageResult data = regenerateCache( report, acceptedPage, key, yieldRate );
      outputStream.write( data.data );
      outputStream.flush();
      return data.pageCount;
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

  private PageResult regenerateCache( final MasterReport report, final int yieldRate, final String key,
                                      final int acceptedPage )
    throws ReportProcessingException {
    logger.warn( "Regenerating report data for " + key );
    final Map<Integer, PageResult> results = produceCachablePages( report, yieldRate );
    boolean first = true;
    for ( final Map.Entry<Integer, PageResult> entry : results.entrySet() ) {
      putPage( key, entry.getKey().intValue(), entry.getValue() );
      if ( first ) {
        putPageCount( key, entry.getValue().pageCount );
        first = false;
      }
    }
    return results.get( acceptedPage );
  }

  private Map<Integer, PageResult> produceCachablePages( final MasterReport report, final int yieldRate )
    throws ReportProcessingException {

    final PageableReportProcessor proc = createReportProcessor( report, yieldRate );

    final PageableHtmlOutputProcessor outputProcessor = (PageableHtmlOutputProcessor) proc.getOutputProcessor();
    outputProcessor.setFlowSelector( new DisplayAllFlowSelector() );

    final Map<Integer, PageResult> result = new HashMap<Integer, PageResult>();
    try {
      final ZipRepository targetRepository = reinitOutputTargetForCaching();
      proc.processReport();
      final int pageCount = proc.getLogicalPageCount();
      final ContentLocation root = targetRepository.getRoot();
      for ( final ContentEntity contentEntities : root.listContents() ) {
        if ( contentEntities instanceof ContentItem ) {
          final ContentItem ci = (ContentItem) contentEntities;
          final String name = ci.getName();
          final int pageNumber = extractPageFromName( name );
          if ( pageNumber >= 0 ) {
            final PageResult r = new PageResult( pageCount, read( ci.getInputStream() ) );
            result.put( pageNumber, r );
          }
        }
      }
      return result;
    } catch ( final ContentIOException e ) {
      return null;
    } catch ( final ReportProcessingException e ) {
      throw e;
    } catch ( final IOException e ) {
      return null;
    }
  }

  private byte[] read( final InputStream in ) throws IOException {
    try {
      return IOUtils.toByteArray( in );
    } finally {
      in.close();
    }
  }

  private int generateNonCaching( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                                  final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    return super.generate( report, acceptedPage, outputStream, yieldRate );
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

  public Integer getPageCount( final String key ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final ICacheManager mgr = PentahoSystem.getCacheManager( session );
    final Object fromSessionCache = mgr.getFromSessionCache( session, key + "/pages" );
    if ( fromSessionCache instanceof Integer ) {
      return (Integer) fromSessionCache;
    }
    return null;
  }

  private void putPageCount( final String key, final int pageCount ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final ICacheManager mgr = PentahoSystem.getCacheManager( session );
    mgr.putInSessionCache( session, key + "/pages", Integer.valueOf( pageCount ) );
  }

  public PageResult getPage( final String key, final int page ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final ICacheManager mgr = PentahoSystem.getCacheManager( session );
    final Object fromSessionCache = mgr.getFromSessionCache( session, key + "/page-" + page + ".html" );
    if ( fromSessionCache instanceof PageResult ) {
      return (PageResult) fromSessionCache;
    }
    return null;
  }

  private void putPage( final String key, final int page, final PageResult data ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final ICacheManager mgr = PentahoSystem.getCacheManager( session );
    mgr.putInSessionCache( session, key + "/page-" + page + ".html", data );
  }

  private Serializable computeCacheKey( final MasterReport report ) throws BeanException {
    final ResourceKey definitionSource = report.getDefinitionSource();
    final ArrayList<String> sourceKey = new ArrayList<String>();
    sourceKey.add( String.valueOf( definitionSource.getSchema() ) );
    sourceKey.add( definitionSource.getIdentifierAsString() );
    final HashMap<String, String> params = new HashMap<String, String>();
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
    final ArrayList<Object> key = new ArrayList<Object>();
    key.add( sourceKey );
    key.add( params );
    return key;
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

  public static class PageResult implements Serializable {
    public final int pageCount;
    public final byte[] data;

    private PageResult( final int pageCount, final byte[] data ) {
      this.pageCount = pageCount;
      this.data = data;
    }
  }
}
