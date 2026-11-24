package com.hotel.ui.components;

import com.hotel.model.Room;
import com.hotel.ui.Theme;
import com.hotel.ui.Ui;
import com.hotel.util.Formats;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Clickable card for one room on the overview: number, status pill, type, a context line
 * (current guest, arrival, or "Free tonight") and price. Works with mouse and keyboard,
 * and reports itself to screen readers as a button.
 */
public class RoomCard extends RoundedPanel {

    private final Room room;
    private boolean hovered;

    public RoomCard(Room room, String detail, Consumer<Room> onOpen) {
        super(new BorderLayout(0, 10), 12);
        this.room = room;
        setPadding(14, 16);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel number = new JLabel(room.getRoomNumber());
        number.setFont(Theme.font(Font.BOLD, 20f));
        number.setForeground(Theme.text());
        StatusBadge status = new StatusBadge(room.getStatus().getLabel(), Theme.statusColor(room.getStatus()));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(number, BorderLayout.WEST);
        top.add(status, BorderLayout.EAST);

        JLabel type = Ui.muted(room.getDisplayName() + "  ·  sleeps " + room.getCapacity());
        JLabel context = Ui.label(detail);
        context.setFont(Theme.font(Font.PLAIN, 12.5f));
        JPanel middle = new JPanel(new GridLayout(0, 1, 0, 3));
        middle.setOpaque(false);
        middle.add(type);
        middle.add(context);

        JLabel price = new JLabel("<html><b>" + Formats.moneyCompact(room.getPricePerNight()) + "</b>"
                + "<font color='" + Theme.hex(Theme.mutedText()) + "'>&nbsp;/ night</font></html>");
        price.setForeground(Theme.text());

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(price, BorderLayout.SOUTH);

        getAccessibleContext().setAccessibleName("Room " + room.getRoomNumber() + ", " + room.getStatus().getLabel()
                + ", " + room.getDisplayName() + ", sleeps " + room.getCapacity() + ", " + detail + ", "
                + Formats.money(room.getPricePerNight()) + " per night");
        getAccessibleContext().setAccessibleDescription("Press Enter to see details and actions");
        setToolTipText("<html><b>" + room.getDisplayName() + " amenities</b><br>"
                + String.join("<br>", room.getAmenities()) + "</html>");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    requestFocusInWindow();
                    onOpen.accept(room);
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    onOpen.accept(room);
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
                scrollRectToVisible(new Rectangle(getSize()));
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public Room getRoom() {
        return room;
    }

    @Override
    protected Color borderColor() {
        if (isFocusOwner()) return Theme.focusRing();
        return hovered ? Theme.primary() : Theme.border();
    }

    @Override
    protected float borderWidth() {
        return isFocusOwner() ? 2.5f : hovered ? 1.5f : 1f;
    }

    /** Report this card to assistive technology as a push button, since it acts like one. */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPanel() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }
}
