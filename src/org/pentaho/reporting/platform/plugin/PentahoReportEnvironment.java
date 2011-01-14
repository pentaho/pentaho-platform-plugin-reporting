package org.pentaho.reporting.platform.plugin;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoRequestContextHolder;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.modules.output.csv.CSVQuoter;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;

public class PentahoReportEnvironment extends DefaultReportEnvironment
{
  private static final Log logger = LogFactory.getLog(PentahoReportEnvironment.class);
  private HashMap<String, String> cache;
  private IParameterProvider pathProvider;
  private String clText;
  private String clId;

  public PentahoReportEnvironment(final Configuration configuration)
  {
    this(configuration, null, null);
  }

  public PentahoReportEnvironment(final Configuration configuration,
                                  final String clText,
                                  final String clId)
  {
    super(configuration);
    this.clText = clText;
    this.clId = clId;
  }

  public String getEnvironmentProperty(final String key)
  {
    if (key == null)
    {
      throw new NullPointerException();
    }

    if ("contentLink".equals(key))
    {
      return clText;
    }
    if ("contentLink-widget".equals(key))
    {
      return clId;
    }

    if (cache == null)
    {
      cache = new HashMap<String, String>();
    }
    
    final String cached = cache.get(key);
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
        logger.warn(Messages.getString("ReportPlugin.warnNoApplicationContext"));
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
        final StringBuffer property = new StringBuffer();
        //noinspection unchecked
        final GrantedAuthority[] roles =
            authentication.getAuthorities();
        if (roles == null)
        {
          return null;
        }

        final int rolesSize = roles.length;
        if (rolesSize > 0)
        {
          final CSVQuoter quoter = new CSVQuoter(",");//$NON-NLS-1$
          property.append(roles[0]);
          for (int i = 1; i < rolesSize; i++)
          {
            property.append(",");//$NON-NLS-1$
            property.append(quoter.doQuoting(roles[i].getAuthority()));
          }
        }
        return property.toString();
      }

      if (key.startsWith("session:"))//$NON-NLS-1$
      {
        final Object attribute = session.getAttribute(key.substring("session:".length()));//$NON-NLS-1$
        return String.valueOf(attribute);
      }
    }
    else
    {
      if (key.startsWith("session:") ||//$NON-NLS-1$
          key.equals("username") ||//$NON-NLS-1$
          key.equals("roles"))//$NON-NLS-1$
      {
        logger.warn(Messages.getString("ReportPlugin.warnNoSession"));
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

  public Map<String, String[]> getUrlExtraParameter()
  {
    if (pathProvider == null)
    {
      return super.getUrlExtraParameter();
    }

    final Object maybeRequest = pathProvider.getParameter("httprequest"); // NON-NLS
    if (maybeRequest instanceof HttpServletRequest == false)
    {
      return super.getUrlExtraParameter();
    }

    final HttpServletRequest request = (HttpServletRequest) maybeRequest;
    final Map map = request.getParameterMap();
    final LinkedHashMap<String, String[]> retval = new LinkedHashMap<String, String[]>();
    final Iterator it = map.entrySet().iterator();
    while (it.hasNext())
    {
      final Map.Entry o = (Map.Entry) it.next();
      final String key = (String) o.getKey();
      final String[] values = (String[]) o.getValue();
      retval.put(key, values.clone());
    }
    return retval;
  }
}
