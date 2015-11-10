package org.pentaho.reporting.platform.plugin.cache;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheKey;
import org.pentaho.reporting.platform.plugin.MockTableModel;

import javax.swing.table.TableModel;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class PentahoDataCacheIT {

  private PentahoDataCache dataCache;
  private DataCacheKey dataCacheKey;
  private MockTableModel tableModel;

  @Before
  public void setup() throws Exception {
    createPentahoSession();
    setupDataCache();

    setupDataCacheKey();
    setupTableModel();
  }

  @After
  public void teardown() throws Exception {
    destroyPentahoSession();
  }

  @Test
  public void verifyReturnTableModelShouldBeDifferentThanTheOriginalTableModel() {
    assertNotNull( dataCache.getCacheManager() );
    assertNull( "The data should not be cached.", dataCache.get( dataCacheKey ) );

    TableModel cachedModel = dataCache.put( dataCacheKey, tableModel );

    assertNotNull( "The data should be cached.", dataCache.get( dataCacheKey ) );
    assertNotSame( "The original table model should not be returned.", tableModel, cachedModel );

    Object[][] data = new Object[ 1 ][ 1 ];
    tableModel = new MockTableModel( data );

    cachedModel = dataCache.put( dataCacheKey, tableModel );
    assertSame( tableModel, cachedModel );
  }

  private void createPentahoSession() {
    PentahoSessionHolder.setSession( new StandaloneSession() );
  }

  private void destroyPentahoSession() {
    PentahoSessionHolder.setSession( null );
  }

  private void setupDataCache() {
    dataCache = new PentahoDataCache();
  }

  private void setupDataCacheKey() {
    dataCacheKey = new DataCacheKey();
    dataCacheKey.addParameter( "someParameter1", "someValue1" );
    dataCacheKey.addParameter( "someParameter2", "someValue2" );
  }

  private void setupTableModel() {
    tableModel = new MockTableModel();
  }
}