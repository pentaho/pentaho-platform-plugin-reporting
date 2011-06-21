package org.pentaho.reporting.platform.plugin.connection;

import java.util.ArrayList;
import java.util.Properties;

import javax.sql.DataSource;

import mondrian.olap.Connection;
import org.pentaho.platform.api.engine.IConnectionUserRoleMapper;
import org.pentaho.platform.api.engine.PentahoAccessControlException;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DefaultMondrianConnectionProvider;
import org.pentaho.reporting.libraries.base.util.StringUtils;

public class PentahoMondrianConnectionProvider extends DefaultMondrianConnectionProvider
{
  public static final String MDX_CONNECTION_MAPPER_KEY = "Mondrian-UserRoleMapper"; //$NON-NLS-1$


  public PentahoMondrianConnectionProvider()
  {
  }

  public Connection createConnection(final Properties properties, final DataSource dataSource) throws ReportDataFactoryException
  {
    try
    {
      final String role = properties.getProperty("Role");
      if (StringUtils.isEmpty(role))
      {
        // Only if the action sequence/requester hasn't already injected a role in here do this.
        final String catalog = properties.getProperty("Catalog");
        final String roleFromPentaho = computeRoleString(catalog);
        if (StringUtils.isEmpty(roleFromPentaho) == false)
        {
          properties.setProperty("Role", roleFromPentaho);
        }
      }
      return super.createConnection(properties, dataSource);
    }
    catch (PentahoAccessControlException e)
    {
      throw new ReportDataFactoryException("Failed to map roles", e);
    }
  }

  private String computeRoleString(final String catalog) throws PentahoAccessControlException
  {
    if(PentahoSystem.getObjectFactory().objectDefined(MDX_CONNECTION_MAPPER_KEY))
    {
      final IConnectionUserRoleMapper mondrianUserRoleMapper =
          PentahoSystem.get(IConnectionUserRoleMapper.class, MDX_CONNECTION_MAPPER_KEY, null);
      if (mondrianUserRoleMapper != null)
      {
        // Do role mapping
        final String[] validMondrianRolesForUser =
            mondrianUserRoleMapper.mapConnectionRoles(PentahoSessionHolder.getSession(), catalog);
        if ((validMondrianRolesForUser != null) && (validMondrianRolesForUser.length > 0))
        {
          final StringBuffer buff = new StringBuffer();
          for (int i = 0; i < validMondrianRolesForUser.length; i++)
          {
            final String aRole = validMondrianRolesForUser[i];
            // According to http://mondrian.pentaho.org/documentation/configuration.php
            // double-comma escapes a comma
            if (i > 0)
            {
              buff.append(",");
            }
            buff.append(aRole.replaceAll(",", ",,"));
          }
          return buff.toString();
        }
      }
    }
    return null;
  }

  public Object getConnectionHash(final Properties properties) throws ReportDataFactoryException
  {
    try
    {
      final ArrayList<Object> hash = (ArrayList<Object>) super.getConnectionHash(properties);
      final String catalog = properties.getProperty("Catalog");
      hash.add(computeRoleString(catalog));
      return hash;
    }
    catch (PentahoAccessControlException e)
    {
      throw new ReportDataFactoryException("Failed to map roles", e);
    }
  }
}
