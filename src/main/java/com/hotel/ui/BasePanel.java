package com.hotel.ui;

import com.hotel.controller.ValidationException;
import com.hotel.ui.components.ScrollablePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Parent of every page in the dashboard. It draws the common page header; each subclass
 * overrides {@link #getTitle()} and {@link #refresh()}, and the dashboard calls them
 * without knowing which page it is talking to (polymorphism).
 */
public abstract class BasePanel extends JPanel {

    /** Work that may fail with a validation or database error. */
    @FunctionalInterface
    protected interface Task {
        void run() throws Exception;
    }

    /** Right side of the page header, for page-specific tools. */
    protected final JPanel headerTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    protected BasePanel() {
        super(new BorderLayout(0, 16));
        setBackground(Theme.background());
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.heading(getTitle(), 22f));
        if (!getSubtitle().isEmpty()) {
            titles.add(Ui.muted(getSubtitle()));
        }
        headerTools.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(headerTools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        getAccessibleContext().setAccessibleName(getTitle());
    }

    /**
     * Puts the page content in a vertical scroll area. Sections keep their natural height
     * (nothing gets squashed); a scroll bar appears only when the window is too short.
     */
    protected void setScrollableContent(JComponent content) {
        ScrollablePanel holder = new ScrollablePanel(new BorderLayout());
        holder.setBorder(new EmptyBorder(0, 0, 0, 4));  // keep clear of the scroll bar
        holder.add(content, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    public abstract String getTitle();

    public String getSubtitle() {
        return "";
    }

    /** Reloads the page's data from the database. */
    public abstract void refresh();

    /**
     * Runs a task and shows any problem in a dialog.
     *
     * @return true if the task finished without errors
     */
    protected final boolean attempt(Task task) {
        try {
            task.run();
            return true;
        } catch (ValidationException e) {
            Dialogs.warn(this, e.getMessage());
        } catch (Exception e) {
            Dialogs.error(this, e);
        }
        return false;
    }
}
