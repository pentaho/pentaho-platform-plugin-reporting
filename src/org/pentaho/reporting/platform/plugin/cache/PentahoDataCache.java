package org.pentaho.reporting.platform.plugin.cache;

import javax.swing.table.TableModel;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.engine.classic.core.cache.DataCache;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheKey;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheManager;
import org.pentaho.reporting.engine.classic.core.cache.InMemoryDataCache;

/**
 * A simple data cache that wraps around the plain in-memory data-cache. That cache is stored on the user's
 * session and shared across all reports run by that user in that session.
 *
 * @author Thomas Morgner.
 */
public class PentahoDataCache implements DataCache
{
  private static String SESSION_ATTRIBUTE = PentahoDataCache.class.getName() + "-DataCache";

  private static class PentahoDataCacheManager implements DataCacheManager
  {
    private PentahoDataCacheManager()
    {
    }

    public void clearAll()
    {
      final IPentahoSession session = PentahoSessionHolder.getSession();
      final DataCache dataCache = (DataCache) session.getAttribute(SESSION_ATTRIBUTE);
      if (dataCache == null)
      {
        return;
      }

      dataCache.getCacheManager().clearAll();
    }

    public void shutdown()
    {
      final IPentahoSession session = PentahoSessionHolder.getSession();
      final DataCache dataCache = (DataCache) session.getAttribute(SESSION_ATTRIBUTE);
      if (dataCache == null)
      {
        return;
      }

      dataCache.getCacheManager().shutdown();
    }
  }

  private PentahoDataCacheManager manager;

  public PentahoDataCache()
  {
    manager = new PentahoDataCacheManager();
  }

  public synchronized TableModel get(final DataCacheKey key)
  {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final DataCache dataCache = (DataCache) session.getAttribute(SESSION_ATTRIBUTE);
    if (dataCache == null)
    {
      return null;
    }

    return dataCache.get(key);
  }

  public synchronized TableModel put(final DataCacheKey key, final TableModel model)
  {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    DataCache dataCache = (DataCache) session.getAttribute(SESSION_ATTRIBUTE);
    if (dataCache == null)
    {
      dataCache = new InMemoryDataCache();
      session.setAttribute(SESSION_ATTRIBUTE, dataCache);
    }
    return dataCache.put(key, model);
  }

  public DataCacheManager getCacheManager()
  {
    return manager;
  }
}
