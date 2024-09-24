package org.pentaho.reporting.platform.plugin;

import com.google.common.base.MoreObjects;
import org.pentaho.reporting.engine.classic.core.util.CloseableTableModel;

import javax.swing.table.AbstractTableModel;

public class MockTableModel extends AbstractTableModel implements CloseableTableModel {

    Object[][] data;
    int rowCount = 0;
    int columnCount = 0;
    boolean closed;

    public MockTableModel(final Object[][] data) {
        this.data = MoreObjects.firstNonNull(data, new Object[0][0]);
        rowCount = this.data.length;
        for (int i = 0; i < this.data.length; i++) {
            columnCount = Math.max(columnCount, this.data[i].length);
        }
    }

    public MockTableModel() {
        this(null);
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return data[rowIndex][columnIndex];
    }

}
