package org.pentaho.reporting.platform.plugin.datasources;

import javax.swing.table.TableModel;

import org.pentaho.metadata.query.model.Query;
import org.pentaho.metadata.query.model.util.QueryXmlHelper;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ResourceBundleFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.SimplePmdDataFactory;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.util.ObjectUtilities;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;


public class QuerylessDataFactory extends PmdDataFactory {

  private static final long serialVersionUID = -1936056093763715852L;

  private Configuration configuration;

  public void initialize(final Configuration pConfiguration,
      final ResourceManager resourceManager,
      final ResourceKey contextKey,
      final ResourceBundleFactory resourceBundleFactory) {

    this.configuration = pConfiguration;
    super.initialize(pConfiguration, resourceManager, contextKey, resourceBundleFactory);
  }
  
  @Override
  public void cancelRunningQuery() {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isQueryExecutable(String queryName, DataRow arg1) {
    boolean ok = getQuery(queryName) != null;
    return ok;
  }

  @Override
  public void open() throws ReportDataFactoryException {
  }

  @Override
  public TableModel queryData(String queryName, DataRow parameters) throws ReportDataFactoryException {

    final String query = getQuery(queryName);
    if (query == null)
    {
      throw new ReportDataFactoryException("No such query: " + queryName); //$NON-NLS-1$
    }
    final Query queryObject = parseQuery(query);

    QuerylessTableModel table = new QuerylessTableModel();
    table.setQuery(queryObject);
    return table;
  }

  private Query parseQuery(final String query) throws ReportDataFactoryException
  {
    final String xmlHelperClass = configuration.getConfigProperty
        ("org.pentaho.reporting.engine.classic.extensions.datasources.pmd.XmlHelperClass"); //$NON-NLS-1$

    final QueryXmlHelper helper =
        (QueryXmlHelper) ObjectUtilities.loadAndInstantiate(xmlHelperClass, SimplePmdDataFactory.class, QueryXmlHelper.class);
    if (helper == null) {
      throw new ReportDataFactoryException("Failed to create XmlHelper: " + xmlHelperClass);//$NON-NLS-1$
    }

    try
    {
      // never returns null
      return helper.fromXML(getDomainRepository(), query);
    }
    catch (Exception e)
    {
      throw new ReportDataFactoryException("Failed to parse query", e);//$NON-NLS-1$
    }
  }  
}
