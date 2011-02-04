package org.pentaho.reporting.platform.plugin;

/**
 * Todo: Document me!
 * <p/>
 * Date: 03.02.11
 * Time: 15:52
 *
 * @author Thomas Morgner.
 */
public class ReportSessionIdHolder
{
  private static ThreadLocal<String> sessionId = new ThreadLocal<String>();

  public static String get()
  {
    return sessionId.get();
  }

  public static void set(final String value)
  {
    sessionId.set(value);
  }

  public static void remove()
  {
    sessionId.remove();
  }
}
