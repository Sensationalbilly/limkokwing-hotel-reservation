package com.hotel.ui;

import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.StatusCellRenderer;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Predicate;

/** Factory methods so every screen builds labels, sections and tables the same way. */
public final class Ui {

    private Ui() { }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.text());
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.mutedText());
        label.setFont(Theme.font(Font.PLAIN, 12f));
        return label;
    }

    public static JLabel heading(String text, float size) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.text());
        label.setFont(Theme.font(Font.BOLD, size));
        return label;
    }

    public static JCheckBox checkBox(String text, int mnemonic) {
        JCheckBox box = new JCheckBox(text);
        box.setOpaque(false);
        box.setForeground(Theme.text());
        if (mnemonic != 0) box.setMnemonic(mnemonic);
        return box;
    }

    public static JRadioButton radio(String text, int mnemonic) {
        JRadioButton button = new JRadioButton(text);
        button.setOpaque(false);
        button.setForeground(Theme.text());
        if (mnemonic != 0) button.setMnemonic(mnemonic);
        return button;
    }

    /** A white card with a heading, used to group related controls. */
    public static JPanel section(String title, JComponent body) {
        return section(heading(title, 15f), body);
    }

    public static JPanel section(JLabel title, JComponent body) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.surface());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border()),
                new EmptyBorder(14, 16, 14, 16)));
        panel.add(title, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        panel.getAccessibleContext().setAccessibleName(title.getText());
        return panel;
    }

    public static JPanel flow(int align, JComponent... items) {
        JPanel panel = new JPanel(new FlowLayout(align, 8, 0));
        panel.setOpaque(false);
        for (JComponent item : items) {
            panel.add(item);
        }
        return panel;
    }

    /** Stacks components top to bottom, each stretched to full width. */
    public static JPanel stack(int gap, JComponent... items) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < items.length; i++) {
            c.gridy = i;
            c.insets = new Insets(i == 0 ? 0 : gap, 0, 0, 0);
            panel.add(items[i], c);
        }
        c.gridy = items.length;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(Box.createGlue(), c);
        return panel;
    }

    /** Wraps a component at a fixed width; its height always follows the content. */
    public static JPanel fixedWidth(JComponent component, int width) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, component.getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(Theme.border()));
        pane.getViewport().setBackground(Theme.surface());
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    public static void styleTable(JTable table, String accessibleName) {
        table.setRowHeight(Theme.scaled(30));
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.border());
        table.setBackground(Theme.surface());
        table.setForeground(Theme.text());
        table.setSelectionBackground(Theme.primary());
        table.setSelectionForeground(Theme.onAccent());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 13f));
        table.setDefaultRenderer(BigDecimal.class, new MoneyRenderer());
        table.setDefaultRenderer(LocalDate.class, new DateRenderer());
        table.setDefaultRenderer(RoomStatus.class, new StatusCellRenderer());
        table.setDefaultRenderer(BookingStatus.class, new StatusCellRenderer());
        table.getAccessibleContext().setAccessibleName(accessibleName);
    }

    /** Selects and scrolls to the first row matching the condition. */
    public static <T> void selectRow(JTable table, ListTableModel<T> model, Predicate<T> match) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (match.test(model.getRow(i))) {
                int view = table.convertRowIndexToView(i);
                if (view >= 0) {
                    table.setRowSelectionInterval(view, view);
                    table.scrollRectToVisible(table.getCellRect(view, 0, true));
                }
                return;
            }
        }
    }

    /** Sets the tooltip and the screen-reader description in one go. */
    public static void describe(JComponent component, String description) {
        component.setToolTipText(description);
        component.getAccessibleContext().setAccessibleDescription(description);
    }

    private static final class MoneyRenderer extends DefaultTableCellRenderer {
        MoneyRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            setText(value instanceof BigDecimal amount ? Formats.money(amount) : "");
        }
    }

    private static final class DateRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            setText(value instanceof LocalDate date ? Formats.date(date) : "");
        }
    }
}
