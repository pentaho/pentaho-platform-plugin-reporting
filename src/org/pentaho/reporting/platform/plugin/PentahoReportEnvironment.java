package org.pentaho.reporting.platform.plugin;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserDetailsRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.modules.output.csv.CSVQuoter;
import org.pentaho.reporting.libraries.base.config.Configuration;

public class PentahoReportEnvironment extends DefaultReportEnvironment
{
  public PentahoReportEnvironment(final Configuration configuration)
  {
    super(configuration);
  }

  /**
   * Returns the text encoding that should be used to encode URLs.
   *
   * @return the encoding for URLs.
   */
  public String getURLEncoding()
  {
    return LocaleHelper.getSystemEncoding();
  }

  public String getEnvironmentProperty(final String key)
  {
    if (key == null)
    {
      throw new NullPointerException();
    }

    if (PentahoSystem.getApplicationContext() == null)
    {
      return key;
    }

    final String pentahoBaseURL = PentahoSystem.getApplicationContext().getBaseUrl();

    final IPentahoSession session = PentahoSessionHolder.getSession();
    if ("serverBaseURL".equals(key)) //$NON-NLS-1$
    {
      return getBaseServerURL(pentahoBaseURL);
    }
    else if ("pentahoBaseURL".equals(key)) //$NON-NLS-1$
    {
      return pentahoBaseURL;
    }
    else if ("solutionRoot".equals(key)) //$NON-NLS-1$
    {
      return PentahoSystem.getApplicationContext().getSolutionPath(""); //$NON-NLS-1$
    }
    else if ("hostColonPort".equals(key)) //$NON-NLS-1$
    {
      return getHostColonPort(pentahoBaseURL);
    }
    else if (session != null && "username".equals(key)) //$NON-NLS-1$
    {
      return session.getName();
    }
    else if (session != null && "roles".equals(key)) //$NON-NLS-1$
    {
      final IUserDetailsRoleListService roleListService = PentahoSystem.get(IUserDetailsRoleListService.class);
      if (roleListService == null)
      {
        return null;
      }
      final StringBuffer property = new StringBuffer();
      //noinspection unchecked
      final List<String> roles =
          (List<String>) roleListService.getRolesForUser(session.getName());
      if (roles == null)
      {
        return null;
      }

      final int rolesSize = roles.size();
      if (rolesSize > 0)
      {
        final CSVQuoter quoter = new CSVQuoter(",");
        property.append(roles.get(0));
        for (int i = 1; i < rolesSize; i++)
        {
          property.append(",");
          property.append(quoter.doQuoting(roles.get(i))); //$NON-NLS-1$
        }
      }
      return property.toString();
    }

    if (session != null && key.startsWith("session:"))
    {
      final Object attribute = session.getAttribute(key.substring("session:".length()));
      if (attribute instanceof String)
      {
        return (String) attribute;
      }
      return null;
    }

    return super.getEnvironmentProperty(key);
  }

  private String getBaseServerURL(final String pentahoBaseURL)
  {
    try
    {
      final URL url = new URL(pentahoBaseURL);
      return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort(); //$NON-NLS-1$ //$NON-NLS-2$
    }
    catch (Exception e)
    {
      // ignored
    }
    return pentahoBaseURL;
  }

  private String getHostColonPort(final String pentahoBaseURL)
  {
    try
    {
      final URL url = new URL(pentahoBaseURL);
      return url.getHost() + ":" + url.getPort();//$NON-NLS-1$ 
    }
    catch (Exception e)
    {
      // ignored 
    }
    return pentahoBaseURL;
  }

  public Locale getLocale()
  {
    return LocaleHelper.getLocale();
  }
}
