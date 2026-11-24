package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;

/** Shows room/booking status in bold, coloured text (the word itself carries the meaning). */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setFont(getFont().deriveFont(Font.BOLD));
        if (!isSelected) {
            setForeground(Theme.statusColor(value));
        }
        return this;
    }
}
