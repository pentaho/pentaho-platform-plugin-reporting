package org.pentaho.reporting.platform.plugin.cache;

import net.sf.ehcache.CacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.platform.plugin.MockTableModel;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class DefaultReportCacheIT {
  private DefaultReportCache dataCache;
  private ReportCacheKey dataCacheKey;
  private MockTableModel tableModel;
  StandaloneSession session;

  @Before
  public void setup() throws Exception {
    createPentahoSession();
    setupDataCache();

    setupDataCacheKey( "" );
    setupTableModel();
  }

  @After
  public void teardown() throws Exception {
    destroyPentahoSession();
  }

  @Test
  public void testReturnTableModel() throws Exception {
    setupDataCacheKey( null );
    assertNull( dataCache.get( dataCacheKey ) );

    setupDataCacheKey( "" );
    assertNull( dataCache.get( dataCacheKey ) );

    addAttributeToPentahoSession( "org.pentaho.reporting.platform.plugin.cache.DefaultReportCache-Cache",
        CacheManager.create() );
    assertNull( dataCache.get( dataCacheKey ) );

    ReportOutputHandler report = mock( ReportOutputHandler.class );

    setupDataCacheKey( null );
    assertSame( report, dataCache.put( dataCacheKey, report ) );

    setupDataCacheKey( "" );
    assertNotSame( report, dataCache.put( dataCacheKey, report ) );
  }

  private void createPentahoSession() {
    session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  private void addAttributeToPentahoSession( String name, Object value ) {
    session.setAttribute( name, value );
    PentahoSessionHolder.setSession( session );
  }

  private void destroyPentahoSession() {
    PentahoSessionHolder.setSession( null );
  }

  private void setupDataCache() {
    dataCache = new DefaultReportCache();
  }

  private void setupDataCacheKey( String sessionId ) {
    Map<String, Object> parameter = new HashMap<String, Object>() {{
      put( "someParameter1", "someValue1" );
      put( "someParameter2", "someValue2" );
    }};
    dataCacheKey = new ReportCacheKey( sessionId, parameter );
  }

  private void setupTableModel() {
    tableModel = new MockTableModel();
  }
}
