package org.pentaho.reporting.platform.plugin.datasources;

import javax.swing.table.TableModel;

import org.pentaho.metadata.query.model.Query;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactory;


public class QuerylessDataFactory extends PmdDataFactory {

  private static final long serialVersionUID = -1936056093763715852L;

  @Override
  public void cancelRunningQuery() {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isQueryExecutable(final String queryName, final DataRow arg1) {
    final boolean ok = getQuery(queryName) != null;
    return ok;
  }

  @Override
  public TableModel queryData(final String queryName, final DataRow parameters) throws ReportDataFactoryException {

    final String query = getQuery(queryName);
    if (query == null)
    {
      throw new ReportDataFactoryException("No such query: " + queryName); //$NON-NLS-1$
    }
    final Query queryObject = parseQuery(query);

    final QuerylessTableModel table = new QuerylessTableModel();
    table.setQuery(queryObject);
    return table;
  }
}
