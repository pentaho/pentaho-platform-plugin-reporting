/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2010-2013 Pentaho Corporation.  All rights reserved.
 */

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
