package org.pentaho.reporting.platform.plugin.cache;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

/**
 * This cache stores the report-output-handler on a map inside the user's session.
 *
 * @author Thomas Morgner.
 */
public class DefaultReportCache implements ReportCache
{
  private static String SESSION_ATTRIBUTE = DefaultReportCache.class.getName() + "-Cache";
  private static final String CACHE_NAME = "report-output-handlers";

  private static class LogoutHandler implements ILogoutListener
  {
    private LogoutHandler()
    {
    }

    public void onLogout(final IPentahoSession iPentahoSession)
    {
      final IPentahoSession session = PentahoSessionHolder.getSession();
      final Object attribute = session.getAttribute(SESSION_ATTRIBUTE);
      if (attribute instanceof CacheManager == false)
      {
        return;
      }

      final CacheManager manager = (CacheManager) attribute;
      if (manager.cacheExists(CACHE_NAME))
      {
        final Cache cache = manager.getCache(CACHE_NAME);
        final List keys = new ArrayList(cache.getKeys());
        for (final Object key : keys)
        {
          final Element element = cache.get(key);
          final Object o = element.getValue();
          if (o instanceof CacheHolder)
          {
            final CacheHolder cacheHolder = (CacheHolder) o;
            cacheHolder.getOutputHandler().close();
            cache.remove(key);
          }
        }
      }

      manager.shutdown();
    }
  }

  private static class CacheHolder
  {
    private ReportCacheKey realKey;
    private ReportOutputHandler outputHandler;
    private boolean closed;

    private CacheHolder(final ReportCacheKey realKey, final ReportOutputHandler outputHandler)
    {
      this.realKey = realKey;
      this.outputHandler = outputHandler;
    }

    public ReportCacheKey getRealKey()
    {
      return realKey;
    }

    public ReportOutputHandler getOutputHandler()
    {
      return outputHandler;
    }

    public void close()
    {
      if (closed == false)
      {
        outputHandler.close();
        closed = true;
      }
    }
  }

  private static class CacheEvictionHandler implements CacheEventListener
  {
    private CacheEvictionHandler()
    {
    }

    public void notifyElementRemoved(final Ehcache ehcache, final Element element) throws CacheException
    {
      final Object o = element.getObjectValue();
      if (o instanceof CacheHolder == false)
      {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.close();
    }

    public void notifyElementPut(final Ehcache ehcache, final Element element) throws CacheException
    {

    }

    public void notifyElementUpdated(final Ehcache ehcache, final Element element) throws CacheException
    {

    }

    public void notifyElementExpired(final Ehcache ehcache, final Element element)
    {
      final Object o = element.getObjectValue();
      if (o instanceof CacheHolder == false)
      {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.close();
    }

    /**
     * This method is called when a element is automatically removed from the cache. We then clear it here.
     *
     * @param ehcache
     * @param element
     */
    public void notifyElementEvicted(final Ehcache ehcache, final Element element)
    {
      final Object o = element.getObjectValue();
      if (o instanceof CacheHolder == false)
      {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.close();
    }

    public void notifyRemoveAll(final Ehcache ehcache)
    {
      // could be that we are to late already here, the javadoc is not very clear on this one ..
      final List keys = new ArrayList(ehcache.getKeys());
      for (final Object key : keys)
      {
        final Element element = ehcache.get(key);
        final Object o = element.getValue();
        if (o instanceof CacheHolder)
        {
          final CacheHolder cacheHolder = (CacheHolder) o;
          cacheHolder.close();
        }
      }
    }

    public Object clone() throws CloneNotSupportedException
    {
      return super.clone();
    }

    public void dispose()
    {

    }
  }

  public DefaultReportCache()
  {
  }

  public ReportOutputHandler get(final ReportCacheKey key)
  {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    synchronized (session)
    {
      final Object attribute = session.getAttribute(SESSION_ATTRIBUTE);
      if (attribute instanceof CacheManager == false)
      {
        return null;
      }

      final CacheManager manager = (CacheManager) attribute;
      if (manager.cacheExists(CACHE_NAME) == false)
      {
        return null;
      }

      final Cache cache = manager.getCache(CACHE_NAME);
      final Element element = cache.get(key.getSessionId());
      final Object o = element.getObjectValue();
      if (o instanceof CacheHolder == false)
      {
        return null;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      if (cacheHolder.getRealKey().equals(key) == false)
      {
        cache.remove(key.getSessionId());
        return null;
      }
      return cacheHolder.getOutputHandler();
    }
  }

  public void put(final ReportCacheKey key, final ReportOutputHandler report)
  {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    synchronized (session)
    {
      final Object attribute = session.getAttribute(SESSION_ATTRIBUTE);
      final CacheManager manager;
      if (attribute instanceof CacheManager == false)
      {
        manager = new CacheManager();
        session.setAttribute(SESSION_ATTRIBUTE, manager);
        PentahoSystem.addLogoutListener(new LogoutHandler());
      }
      else
      {
        manager = (CacheManager) attribute;
      }

      if (manager.cacheExists(CACHE_NAME) == false)
      {
        manager.addCache(CACHE_NAME);
        final Cache cache = manager.getCache(CACHE_NAME);
        cache.getCacheEventNotificationService().registerListener(new CacheEvictionHandler());
      }

      final Cache cache = manager.getCache(CACHE_NAME);
      final Element element = cache.get(key.getSessionId());
      final Object o = element.getObjectValue();
      if (o != null)
      {
        cache.remove(key.getSessionId());
      }

      final CacheHolder cacheHolder = new CacheHolder(key, report);
      cache.put(new Element(key, cacheHolder));
    }
  }
}
