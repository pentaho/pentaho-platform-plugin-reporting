package org.pentaho.reporting.platform.plugin;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoRequestContextHolder;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.util.CSVQuoter;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;

public class PentahoReportEnvironment extends DefaultReportEnvironment
{
  private static final Log logger = LogFactory.getLog(PentahoReportEnvironment.class);
  private HashMap<String, Object> cache;
  private String clText;

  public PentahoReportEnvironment(final Configuration configuration)
  {
    this(configuration, null);
  }

  public PentahoReportEnvironment(final Configuration configuration,
                                  final String clText)
  {
    super(configuration);
    this.clText = clText;
  }

  public Object getEnvironmentProperty(final String key)
  {
    if (key == null)
    {
      throw new NullPointerException();
    }

    if ("contentLink".equals(key))
    {
      return clText;
    }

    if (cache == null)
    {
      cache = new HashMap<String, Object>();
    }

    final Object cached = cache.get(key);
    if (cached != null)
    {
      return cached;
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();
    if (PentahoSystem.getApplicationContext() != null)
    {
      final String fullyQualifiedServerUrl = PentahoSystem.getApplicationContext().getFullyQualifiedServerURL();
      if ("serverBaseURL".equals(key)) //$NON-NLS-1$
      {
        final String baseServerURL = getBaseServerURL(fullyQualifiedServerUrl);
        cache.put(key, baseServerURL);
        return baseServerURL;
      }
      else if ("pentahoBaseURL".equals(key)) //$NON-NLS-1$
      {
        cache.put(key, fullyQualifiedServerUrl);
        return fullyQualifiedServerUrl;
      }
      else if ("solutionRoot".equals(key)) //$NON-NLS-1$
      {
        final String solutionRoot = PentahoSystem.getApplicationContext().getSolutionPath("");
        cache.put(key, solutionRoot);
        return solutionRoot; //$NON-NLS-1$
      }
      else if ("hostColonPort".equals(key)) //$NON-NLS-1$
      {
        final String hostColonPort = getHostColonPort(fullyQualifiedServerUrl);
        cache.put(key, hostColonPort);
        return hostColonPort;
      }
      else if ("requestContextPath".equals(key)) //$NON-NLS-1$
      {
        final String requestContextPath = PentahoRequestContextHolder.getRequestContext().getContextPath();
        cache.put(key, requestContextPath);
      }
    }
    else
    {
      if ("serverBaseURL".equals(key) || //$NON-NLS-1$
          "pentahoBaseURL".equals(key) || //$NON-NLS-1$
          "solutionRoot".equals(key) || //$NON-NLS-1$
          "hostColonPort".equals(key) ||//$NON-NLS-1$
          "requestContextPath".equals(key) //$NON-NLS-1$
          ) //$NON-NLS-1$
      {
        logger.warn(Messages.getInstance().getString("ReportPlugin.warnNoApplicationContext"));
        // make it explicit that these values are not available. This way
        // a configuration in the classic-engine.properties file cannot begin
        // to interfer here.
        cache.put(key, null);
        return null;
      }
    }
    if (session != null)
    {
      if ("username".equals(key)) //$NON-NLS-1$
      {
        final Authentication authentication = SecurityHelper.getAuthentication(session, true);
        final String userName = authentication.getName();
        cache.put(key, userName);
        return userName;
      }
      else if ("roles".equals(key)) //$NON-NLS-1$
      {
        final Authentication authentication = SecurityHelper.getAuthentication(session, true);
        final StringBuilder property = new StringBuilder();
        final GrantedAuthority[] roles = authentication.getAuthorities();
        if (roles == null)
        {
          return null;
        }

        final int rolesSize = roles.length;
        final CSVQuoter quoter = new CSVQuoter(',', '"');//$NON-NLS-1$
        for (int i = 0; i < rolesSize; i++)
        {
          if (i != 0)
          {
            property.append(",");//$NON-NLS-1$
          }
          property.append(quoter.doQuoting(roles[i].getAuthority()));
        }
        return property.toString();
      }

      if (key.startsWith("session:"))//$NON-NLS-1$
      {
        return session.getAttribute(key.substring("session:".length()));//$NON-NLS-1$
      }
      else if (key.startsWith("global:"))
      {
        return PentahoSystem.getGlobalParameters().getParameter(key.substring("global:".length()));//$NON-NLS-1$
      }
      
    }
    else
    {
      if (key.startsWith("session:") ||//$NON-NLS-1$
          key.equals("username") ||//$NON-NLS-1$
          key.equals("roles"))//$NON-NLS-1$
      {
        logger.warn(Messages.getInstance().getString("ReportPlugin.warnNoSession"));
        return null;
      }
    }

    return super.getEnvironmentProperty(key);
  }

  private String getBaseServerURL(final String fullyQualifiedServerUrl)
  {
    try
    {
      final URL url = new URL(fullyQualifiedServerUrl);
      return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort(); //$NON-NLS-1$ //$NON-NLS-2$
    }
    catch (Exception e)
    {
      // ignore
    }
    return fullyQualifiedServerUrl;
  }

  private String getHostColonPort(final String fullyQualifiedServerUrl)
  {
    try
    {
      final URL url = new URL(fullyQualifiedServerUrl);
      return url.getHost() + ":" + url.getPort();//$NON-NLS-1$
    }
    catch (Exception e)
    {
      // ignore
    }
    return fullyQualifiedServerUrl;
  }

  public Locale getLocale()
  {
    return LocaleHelper.getLocale();
  }
}
