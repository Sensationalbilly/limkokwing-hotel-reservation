package com.hotel.ui;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.ModalCard;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.ui.components.StatusBadge;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Animated pop-up with a room's details and the actions staff can take on it. */
public class RoomDetailsDialog extends ModalCard {

    public enum Action { BOOK, MAINTENANCE, AVAILABLE }

    public RoomDetailsDialog(Window owner, Room room, String rightNow, boolean canBook, boolean canChangeStatus) {
        super(owner, "Room " + room.getRoomNumber(),
                room.getDisplayName() + " room  ·  " + (room.getFloor() == 0 ? "Ground floor" : "Floor " + room.getFloor()));
        addHeaderBadge(new StatusBadge(room.getStatus().getLabel(), Theme.statusColor(room.getStatus())));

        JPanel facts = new JPanel(new GridLayout(1, 3, 10, 0));
        facts.setOpaque(false);
        facts.add(tile("Sleeps", room.getCapacity() + (room.getCapacity() == 1 ? " guest" : " guests")));
        facts.add(tile("Per night", Formats.moneyCompact(room.getPricePerNight())));
        facts.add(tile("Breakfast", room.includesBreakfast() ? "Included" : "Optional extra"));

        JPanel amenities = new JPanel(new GridLayout(0, 2, 16, 6));
        amenities.setOpaque(false);
        String tick = "<font face='Segoe UI Symbol' color='" + Theme.hex(Theme.success()) + "'>✓</font>&nbsp;&nbsp;";
        for (String amenity : room.getAmenities()) {
            JLabel item = new JLabel("<html>" + tick + amenity + "</html>");
            item.setForeground(Theme.text());
            item.getAccessibleContext().setAccessibleName(amenity);
            amenities.add(item);
        }

        JPanel body = Ui.stack(18,
                facts,
                tile("Right now", rightNow),
                section("Amenities", amenities));
        if (!room.getDescription().isBlank()) {
            JLabel notes = Ui.label("<html>" + escape(room.getDescription()) + "</html>");
            body = Ui.stack(18, body, section("Notes", notes));
        }
        body.setPreferredSize(new Dimension(Theme.scaled(500), body.getPreferredSize().height));
        setBody(body);

        if (canChangeStatus && room.getStatus() == RoomStatus.AVAILABLE) {
            addAction(new BaseButton("Mark as maintenance"), Action.MAINTENANCE, true)
                    .setMnemonic('M');
        } else if (canChangeStatus && room.getStatus() == RoomStatus.MAINTENANCE) {
            addAction(new BaseButton("Mark as available"), Action.AVAILABLE, true)
                    .setMnemonic('M');
        }
        addAction(new BaseButton("Close"), null, false);
        if (canBook) {
            PrimaryButton book = addAction(new PrimaryButton("Book this room"), Action.BOOK, false);
            book.setMnemonic('B');
            setDefaultAction(book);
        }
    }

    /** Shows the dialog and returns what the user chose, or null if they closed it. */
    public Action open() {
        return (Action) showCard();
    }

    /** Small grey box with a caption and a value. */
    private static JComponent tile(String caption, String value) {
        JPanel tile = new JPanel(new BorderLayout(0, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.background());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel valueLabel = Ui.label(value);
        valueLabel.setFont(Theme.font(Font.BOLD, 14.5f));
        tile.add(Ui.muted(caption), BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        tile.getAccessibleContext().setAccessibleName(caption + ": " + value);
        return tile;
    }

    private static JComponent section(String title, JComponent content) {
        JLabel heading = Ui.label(title);
        heading.setFont(Theme.font(Font.BOLD, 13f));
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
