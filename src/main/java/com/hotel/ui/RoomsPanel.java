package com.hotel.ui;

import com.hotel.controller.EventBus;
import com.hotel.controller.RoomController;
import com.hotel.controller.Session;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DangerButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.util.Formats;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Room inventory. Administrators can add, edit and delete rooms; receptionists can only view. */
public class RoomsPanel extends BasePanel {

    private static final String[] TYPE_NAMES = {"Standard", "Deluxe", "Suite"};

    private final RoomController controller = new RoomController();

    private final ListTableModel<Room> model = new ListTableModel<>(
            new String[]{"Room", "Type", "Floor", "Sleeps", "Price / night", "Status", "Description"},
            new Class<?>[]{String.class, String.class, Integer.class, Integer.class, BigDecimal.class,
                    RoomStatus.class, String.class}) {
        @Override
        protected Object valueAt(Room r, int column) {
            return switch (column) {
                case 0 -> r.getRoomNumber();
                case 1 -> r.getDisplayName();
                case 2 -> r.getFloor();
                case 3 -> r.getCapacity();
                case 4 -> r.getPricePerNight();
                case 5 -> r.getStatus();
                default -> r.getDescription();
            };
        }
    };

    private final JTable table = new JTable(model);

    private final JLabel formTitle = Ui.heading("New room", 15f);
    private final JTextField numberField = new JTextField(12);
    private final JComboBox<String> typeCombo = new JComboBox<>(TYPE_NAMES);
    private final JSpinner floorSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JTextField priceField = new JTextField(12);
    private final JComboBox<RoomStatus> statusCombo = new JComboBox<>(RoomStatus.values());
    private final JTextField descriptionField = new JTextField(12);
    private final Map<String, JCheckBox> amenityBoxes = new LinkedHashMap<>();
    private final JLabel feedback = Ui.muted(" ");
    private boolean loadingRoom;  // stops the Type box from resetting the ticks while a room is loaded
    private final BaseButton newButton = new BaseButton("New");
    private final PrimaryButton saveButton = new PrimaryButton("Save room");
    private final DangerButton deleteButton = new DangerButton("Delete");

    private Room editing;

    public RoomsPanel() {
        Ui.styleTable(table, "Rooms");
        table.setAutoCreateRowSorter(true);

        FormPanel form = new FormPanel();
        form.addField("Room number", KeyEvent.VK_N, numberField);
        form.addField("Type", KeyEvent.VK_T, typeCombo);
        form.addField("Floor", KeyEvent.VK_L, floorSpinner);
        form.addField("Sleeps", KeyEvent.VK_P, capacitySpinner);
        form.addField("Price / night (" + Formats.CURRENCY + ")", KeyEvent.VK_R, priceField);
        form.addField("Status", KeyEvent.VK_U, statusCombo);
        form.addField("Description", KeyEvent.VK_C, descriptionField);
        form.addFullWidth(buildAmenitiesSection());
        form.addFullWidth(feedback);
        form.addFiller();
        Ui.describe(statusCombo, "Occupied is set automatically when a guest checks in");

        newButton.setMnemonic(KeyEvent.VK_W);
        saveButton.setMnemonic(KeyEvent.VK_S);
        deleteButton.setMnemonic(KeyEvent.VK_D);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(Ui.flow(FlowLayout.RIGHT, newButton, deleteButton, saveButton), BorderLayout.SOUTH);
        JPanel editor = Ui.section(formTitle, body);
        
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(Ui.scroll(table), BorderLayout.CENTER);
        content.add(Ui.fixedWidth(editor, Theme.scaled(380)), BorderLayout.EAST);
        setScrollableContent(content);

        wireEvents();
        startNew();
        if (!Session.isAdmin()) {
            makeReadOnly();
        }
        EventBus.subscribe(EventBus.Topic.ROOMS_CHANGED, this::refresh);
    }

    @Override
    public String getTitle() {
        return "Rooms";
    }

    @Override
    public String getSubtitle() {
        return Session.isAdmin() ? "Add rooms, change prices and take rooms out of service"
                : "Room list (read-only: only administrators can edit rooms)";
    }

    @Override
    public void refresh() {
        attempt(() -> {
            model.setRows(controller.getAll());
            if (editing != null) {
                int id = editing.getId();
                Ui.selectRow(table, model, r -> r.getId() == id);
            }
        });
    }

    private void wireEvents() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                load(model.getRow(table.convertRowIndexToModel(row)));
            }
        });
        // Picking a type pre-ticks that type's usual features (each Room subclass has its own defaults).
        typeCombo.addActionListener(e -> {
            if (!loadingRoom) applyTypeDefaults();
        });
        newButton.addActionListener(e -> startNew());
        saveButton.addActionListener(e -> save());
        deleteButton.addActionListener(e -> delete());
    }

    /** Two columns of checkboxes, one per feature, under a small heading. */
    private JComponent buildAmenitiesSection() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 0));
        grid.setOpaque(false);
        for (String amenity : Room.ALL_AMENITIES) {
            JCheckBox box = Ui.checkBox(amenity, 0);
            box.setFont(Theme.font(Font.PLAIN, 12.5f));
            if (amenity.equals(Room.BREAKFAST)) {
                Ui.describe(box, "Ticked: breakfast is free for guests in this room");
            }
            amenityBoxes.put(amenity, box);
            grid.add(box);
        }
        grid.getAccessibleContext().setAccessibleName("Room features");

        JLabel heading = Ui.label("Room features");
        heading.setFont(Theme.font(Font.BOLD, 13f));
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.add(heading, BorderLayout.WEST);
        titleRow.add(Ui.muted("Tick what this room has"), BorderLayout.EAST);

        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.border()),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        section.add(titleRow, BorderLayout.NORTH);
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private void applyTypeDefaults() {
        setTicked(Room.create(Room.TYPES[typeCombo.getSelectedIndex()]).getDefaultAmenities());
    }

    private void setTicked(List<String> amenities) {
        amenityBoxes.forEach((name, box) -> box.setSelected(amenities.contains(name)));
    }

    private List<String> tickedAmenities() {
        return amenityBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void load(Room r) {
        editing = r;
        formTitle.setText("Editing room " + r.getRoomNumber());
        numberField.setText(r.getRoomNumber());
        loadingRoom = true;
        typeCombo.setSelectedIndex(Arrays.asList(Room.TYPES).indexOf(r.getType()));
        loadingRoom = false;
        setTicked(r.getAmenities());
        floorSpinner.setValue(r.getFloor());
        capacitySpinner.setValue(r.getCapacity());
        priceField.setText(r.getPricePerNight().toPlainString());
        statusCombo.setSelectedItem(r.getStatus());
        descriptionField.setText(r.getDescription());
        deleteButton.setEnabled(Session.isAdmin());
        feedback.setText(" ");
    }

    private void startNew() {
        editing = null;
        table.clearSelection();
        formTitle.setText("New room");
        numberField.setText("");
        typeCombo.setSelectedIndex(0);
        floorSpinner.setValue(1);
        capacitySpinner.setValue(2);
        priceField.setText("");
        statusCombo.setSelectedItem(RoomStatus.AVAILABLE);
        descriptionField.setText("");
        deleteButton.setEnabled(false);
        feedback.setText(" ");
        applyTypeDefaults();
    }

    private void save() {
        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            Dialogs.warn(this, "Enter the price as a number, for example 750 or 1200.50.");
            priceField.requestFocusInWindow();
            return;
        }
        Room room = Room.create(Room.TYPES[typeCombo.getSelectedIndex()]);
        room.setId(editing == null ? 0 : editing.getId());
        room.setRoomNumber(numberField.getText());
        room.setFloor((Integer) floorSpinner.getValue());
        room.setCapacity((Integer) capacitySpinner.getValue());
        room.setPricePerNight(price);
        room.setStatus((RoomStatus) statusCombo.getSelectedItem());
        room.setDescription(descriptionField.getText().trim());
        room.setAmenities(tickedAmenities());

        if (attempt(() -> controller.save(room))) {
            editing = room;
            Ui.selectRow(table, model, r -> r.getId() == room.getId());
            formTitle.setText("Editing room " + room.getRoomNumber());
            deleteButton.setEnabled(true);
            feedback.setForeground(Theme.success());
            feedback.setText("Room " + room.getRoomNumber() + " saved.");
        }
    }

    private void delete() {
        if (editing == null) return;
        Room room = editing;
        if (Dialogs.confirm(this, "Delete room " + room.getRoomNumber() + "?",
                "The room will be removed from the hotel. This cannot be undone.", "Delete room", true)) {
            if (attempt(() -> controller.delete(room))) {
                startNew();
            }
        }
    }

    private void makeReadOnly() {
        for (JComponent c : new JComponent[]{numberField, typeCombo, floorSpinner, capacitySpinner, priceField,
                statusCombo, descriptionField, newButton, saveButton, deleteButton}) {
            c.setEnabled(false);
        }
        amenityBoxes.values().forEach(box -> box.setEnabled(false));
        feedback.setText("Only administrators can edit rooms.");
    }
}
