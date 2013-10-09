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

package org.pentaho.reporting.platform.plugin.datasources;

import java.util.Date;
import java.util.Locale;

import javax.swing.event.TableModelListener;

import org.pentaho.metadata.model.LogicalColumn;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metadata.query.model.Query;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.MetaAttributeNames;
import org.pentaho.reporting.engine.classic.core.MetaTableModel;
import org.pentaho.reporting.engine.classic.core.modules.misc.tablemodel.DefaultTableMetaData;
import org.pentaho.reporting.engine.classic.core.util.CloseableTableModel;
import org.pentaho.reporting.engine.classic.core.wizard.DataAttributes;
import org.pentaho.reporting.engine.classic.core.wizard.EmptyDataAttributes;
import org.pentaho.reporting.platform.plugin.messages.Messages;

/**
 * Represents the query for a "queryless" data source. All values within a column must be unique to support using this
 * as a data source for parameter queries. As such, there can only be at most 2 rows of data to support unique
 * {@link java.lang.Boolean} values.
 * 
 */
public class QuerylessTableModel implements CloseableTableModel, MetaTableModel {

  private Query query;

  private DefaultTableMetaData metaData;

  private static final Date DEFAULT_DATE = new Date();

  private static final Double DEFAULT_NUMBER = new Double( 123.45 );

  private static final String DEFAULT_STRING_KEY_FOR_ROW = "QuerylessTableModel.DEFAULT_STRING_VALUE_ROW_"; //$NON-NLS-1$

  // Row Count must not be > 2 to guarantee unique Boolean values
  private static final int DEFAULT_ROW_COUNT = 2;

  public void setQuery( Query query ) {
    this.query = query;

    metaData = new DefaultTableMetaData( query.getSelections().size() );

    for ( int column = 0; column < query.getSelections().size(); column++ ) {
      LogicalColumn col = query.getSelections().get( column ).getLogicalColumn();
      Locale locale = LocaleHelper.getLocale();
      String name = col.getName( locale.toString() );
      metaData.setColumnAttribute( column, MetaAttributeNames.Formatting.NAMESPACE,
          MetaAttributeNames.Formatting.LABEL, name );
      Class<?> clazz = this.getColumnClass( column );
      if ( clazz == Float.class ) {
        metaData.setColumnAttribute( column, MetaAttributeNames.Numeric.NAMESPACE, MetaAttributeNames.Numeric.CURRENCY,
            false );
        metaData.setColumnAttribute( column, MetaAttributeNames.Numeric.NAMESPACE,
                                             MetaAttributeNames.Numeric.SCALE, 0 );
        metaData.setColumnAttribute( column, MetaAttributeNames.Numeric.NAMESPACE, MetaAttributeNames.Numeric.SIGNED,
            false );
        metaData.setColumnAttribute( column, MetaAttributeNames.Numeric.NAMESPACE,
            MetaAttributeNames.Numeric.PRECISION, 2147483647 );
      }
    }
  }

  @Override
  public void addTableModelListener( TableModelListener arg0 ) {
    // nothing to do
  }

  @Override
  public Class<?> getColumnClass( int index ) {
    LogicalColumn column = query.getSelections().get( index ).getLogicalColumn();
    DataType dataType = column.getDataType();

    if ( DataType.BOOLEAN == dataType ) {
      return Boolean.class;
    } else if ( DataType.DATE == dataType ) {
      return Date.class;
    } else if ( DataType.NUMERIC == dataType ) {
      return Float.class;
    }
    return String.class;
  }

  @Override
  public int getColumnCount() {
    return query.getSelections().size();
  }

  @Override
  public String getColumnName( int index ) {
    LogicalColumn col = query.getSelections().get( index ).getLogicalColumn();
    String name = col.getId();
    return name;
  }

  @Override
  public int getRowCount() {
    return DEFAULT_ROW_COUNT;
  }

  @SuppressWarnings( "deprecation" )
  @Override
  public Object getValueAt( int row, int column ) {

    LogicalColumn col = query.getSelections().get( column ).getLogicalColumn();
    DataType dataType = col.getDataType();

    if ( DataType.BOOLEAN == dataType ) {
      return row % 2;
    } else if ( DataType.DATE == dataType ) {
      Date d = new Date( DEFAULT_DATE.getTime() );
      d.setDate( row );
      return d;
    } else if ( DataType.NUMERIC == dataType ) {
      return DEFAULT_NUMBER + row;
    }

    return Messages.getInstance().getString( DEFAULT_STRING_KEY_FOR_ROW + String.valueOf( row ),
        String.valueOf( column ) );
  }

  @Override
  public boolean isCellEditable( int arg0, int arg1 ) {
    return false;
  }

  @Override
  public void removeTableModelListener( TableModelListener arg0 ) {
    // nothing to do
  }

  @Override
  public void setValueAt( Object arg0, int arg1, int arg2 ) {
    // nothing to do
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public DataAttributes getColumnAttributes( final int column ) {
    if ( metaData == null ) {
      return EmptyDataAttributes.INSTANCE;
    }
    return metaData.getColumnAttribute( column );
  }

  @Override
  public DataAttributes getCellDataAttributes( int row, int column ) {
    return null;
  }

  @Override
  public DataAttributes getTableAttributes() {
    return metaData.getTableAttribute();
  }

  @Override
  public boolean isCellDataAttributesSupported() {
    return false;
  }

}
