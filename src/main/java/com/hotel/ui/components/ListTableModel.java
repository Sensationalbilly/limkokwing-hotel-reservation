package com.hotel.ui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model backed by a list of objects. Each table subclasses it and overrides
 * {@link #valueAt(Object, int)} to say what goes in each column.
 */
public abstract class ListTableModel<T> extends AbstractTableModel {

    private final String[] columns;
    private final Class<?>[] columnTypes;
    private List<T> rows = new ArrayList<>();

    protected ListTableModel(String[] columns, Class<?>[] columnTypes) {
        this.columns = columns;
        this.columnTypes = columnTypes;
    }

    protected abstract Object valueAt(T row, int column);

    public void setRows(List<T> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public T getRow(int modelIndex) {
        return rows.get(modelIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columnTypes[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return valueAt(rows.get(rowIndex), columnIndex);
    }
}
