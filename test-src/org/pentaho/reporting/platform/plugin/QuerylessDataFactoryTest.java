/*!
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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.util.Date;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import junit.framework.TestCase;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MetaTableModel;
import org.pentaho.reporting.engine.classic.core.ParameterDataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.designtime.datafactory.DesignTimeDataFactoryContext;
import org.pentaho.reporting.engine.classic.core.util.CloseableTableModel;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdConnectionProvider;
import org.pentaho.reporting.libraries.base.boot.ModuleInitializeException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.datasources.QuerylessDataFactory;
import org.pentaho.reporting.platform.plugin.messages.Messages;

@SuppressWarnings( { "all" } )
public class QuerylessDataFactoryTest extends TestCase implements TableModelListener {

  public void setUp() {
    ClassicEngineBoot.getInstance().start();
  }

  public void testQuerylessDataFactory() throws ReportDataFactoryException, KettleException, ModuleInitializeException,
    ResourceException, IOException {
    final String queryName = "query";
    final String queryString =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><mql><domain_id>steel-wheels</domain_id>"
           + "<model_id>BV_ORDERS</model_id><options><disable_distinct>false</disable_distinct></options>"
           + "<selections><selection><view>CAT_PRODUCTS</view><column>BC_PRODUCTS_PRODUCTLINE</column>"
           + "<aggregation>NONE</aggregation></selection><selection><view>CAT_PRODUCTS</view>"
           + "<column>BC_PRODUCTS_PRODUCTNAME</column><aggregation>NONE</aggregation></selection>"
           + "<selection><view>CAT_PORDERS</view><column>BC_ORDERS_ORDERDATE</column>"
           + "<aggregation>NONE</aggregation></selection><selection><view>CAT_ORDERS</view>"
           + "<column>BC_ORDERDETAILS_QUANTITYORDERED</column><aggregation>SUM</aggregation></selection>"
           + "<selection><view>CAT_ORDERS</view><column>BC_ORDERDETAILS_TOTAL</column>"
           + "<aggregation>SUM</aggregation></selection></selections><constraints/><orders/></mql>";
    final QuerylessDataFactory qdf = new QuerylessDataFactory();
    qdf.setDomainId( "steel-wheels" );
    qdf.setQuery( queryName, queryString, null, null );
    qdf.setXmiFile( "resource/solution/test/metadata.xmi" );
    qdf.initialize( new DesignTimeDataFactoryContext() );
    qdf.setConnectionProvider( new PmdConnectionProvider() );

    final CloseableTableModel tableModel = (CloseableTableModel) qdf.queryData( queryName, new ParameterDataRow() );
    assertNotNull( tableModel );

    assertFalse( ( (MetaTableModel) tableModel ).isCellDataAttributesSupported() );
    assertNull( ( (MetaTableModel) tableModel ).getCellDataAttributes( 0, 0 ) );

    assertEquals( 5, tableModel.getColumnCount() );
    assertEquals( 2, tableModel.getRowCount() );
    assertEquals( "BC_PRODUCTS_PRODUCTLINE", tableModel.getColumnName( 0 ) );
    assertEquals( "BC_PRODUCTS_PRODUCTNAME", tableModel.getColumnName( 1 ) );
    assertEquals( "BC_ORDERS_ORDERDATE", tableModel.getColumnName( 2 ) );
    assertEquals( "BC_ORDERDETAILS_QUANTITYORDERED", tableModel.getColumnName( 3 ) );
    assertEquals( "BC_ORDERDETAILS_TOTAL", tableModel.getColumnName( 4 ) );

    assertEquals( String.class, tableModel.getColumnClass( 0 ) );
    assertEquals( String.class, tableModel.getColumnClass( 1 ) );
    assertEquals( Date.class, tableModel.getColumnClass( 2 ) );
    assertEquals( Float.class, tableModel.getColumnClass( 3 ) );
    assertEquals( Float.class, tableModel.getColumnClass( 4 ) );

    assertEquals( Messages.getInstance().getString( "QuerylessTableModel.DEFAULT_STRING_VALUE_ROW_0", "0" ), tableModel
        .getValueAt( 0, 0 ) );
    assertEquals( Messages.getInstance().getString( "QuerylessTableModel.DEFAULT_STRING_VALUE_ROW_0", "1" ), tableModel
        .getValueAt( 0, 1 ) );
    assertEquals( Date.class, tableModel.getValueAt( 0, 2 ).getClass() );
    assertTrue( new Double( 123.45 ).equals( tableModel.getValueAt( 0, 3 ) ) );
    assertTrue( new Double( 123.45 ).equals( tableModel.getValueAt( 0, 4 ) ) );

    assertFalse( tableModel.isCellEditable( 0, 0 ) );
    // these should not blow up
    tableModel.addTableModelListener( this );
    tableModel.close();
    tableModel.removeTableModelListener( this );
    tableModel.setValueAt( this, 0, 0 );

    qdf.close();

  }

  @Override
  public void tableChanged( TableModelEvent arg0 ) {
    // TODO Auto-generated method stub

  }

}
