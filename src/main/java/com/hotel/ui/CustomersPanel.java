package com.hotel.ui;

import com.hotel.controller.CustomerController;
import com.hotel.controller.EventBus;
import com.hotel.model.Customer;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DangerButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.PrimaryButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.regex.Pattern;

/** Guest records: searchable table on the left, edit form on the right. */
public class CustomersPanel extends BasePanel {

    private final CustomerController controller = new CustomerController();

    private final ListTableModel<Customer> model = new ListTableModel<>(
            new String[]{"Name", "Phone", "Email", "ID / Passport", "Nationality"},
            new Class<?>[]{String.class, String.class, String.class, String.class, String.class}) {
        @Override
        protected Object valueAt(Customer c, int column) {
            return switch (column) {
                case 0 -> c.getFullName();
                case 1 -> c.getPhone();
                case 2 -> c.getEmail();
                case 3 -> c.getIdNumber();
                default -> c.getNationality();
            };
        }
    };

    private final JTable table = new JTable(model);
    private final TableRowSorter<ListTableModel<Customer>> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);

    private final JLabel formTitle = Ui.heading("New guest", 15f);
    private final JTextField nameField = new JTextField(16);
    private final JTextField phoneField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField idField = new JTextField(16);
    private final JComboBox<String> nationalityCombo = new JComboBox<>(Customer.COMMON_NATIONALITIES);
    private final JLabel feedback = Ui.muted(" ");
    private final BaseButton newButton = new BaseButton("New");
    private final PrimaryButton saveButton = new PrimaryButton("Save guest");
    private final DangerButton deleteButton = new DangerButton("Delete");

    private Customer editing;

    public CustomersPanel() {
        JLabel searchLabel = Ui.label("Search:");
        searchLabel.setDisplayedMnemonic(KeyEvent.VK_E);
        searchLabel.setLabelFor(searchField);
        Ui.describe(searchField, "Filter guests by any column");
        headerTools.add(searchLabel);
        headerTools.add(searchField);

        Ui.styleTable(table, "Guests");
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);

        nationalityCombo.setEditable(true);
        FormPanel form = new FormPanel();
        form.addField("Full name", KeyEvent.VK_N, nameField);
        form.addField("Phone", KeyEvent.VK_P, phoneField);
        form.addField("Email", KeyEvent.VK_M, emailField);
        form.addField("ID / passport", KeyEvent.VK_I, idField);
        form.addField("Nationality", KeyEvent.VK_T, nationalityCombo);
        form.addFullWidth(feedback);
        form.addFiller();

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
        EventBus.subscribe(EventBus.Topic.CUSTOMERS_CHANGED, this::refresh);
    }

    @Override
    public String getTitle() {
        return "Guests";
    }

    @Override
    public String getSubtitle() {
        return "Guest profiles and contact details";
    }

    @Override
    public void refresh() {
        attempt(() -> {
            model.setRows(controller.getAll());
            if (editing != null) {
                int id = editing.getId();
                Ui.selectRow(table, model, c -> c.getId() == id);
            }
        });
    }

    private void wireEvents() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                load(model.getRow(table.convertRowIndexToModel(row)));
            }
        });

        newButton.addActionListener(e -> startNew());
        saveButton.addActionListener(e -> save());
        deleteButton.addActionListener(e -> delete());
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    private void load(Customer c) {
        editing = c;
        formTitle.setText("Editing: " + c.getFullName());
        nameField.setText(c.getFullName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        idField.setText(c.getIdNumber());
        nationalityCombo.setSelectedItem(c.getNationality());
        deleteButton.setEnabled(true);
        feedback.setText(" ");
    }

    private void startNew() {
        editing = null;
        table.clearSelection();
        formTitle.setText("New guest");
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        idField.setText("");
        nationalityCombo.setSelectedIndex(0);
        deleteButton.setEnabled(false);
        feedback.setText(" ");
        nameField.requestFocusInWindow();
    }

    private void save() {
        Customer c = new Customer();
        c.setId(editing == null ? 0 : editing.getId());
        c.setFullName(nameField.getText());
        c.setPhone(phoneField.getText());
        c.setEmail(emailField.getText());
        c.setIdNumber(idField.getText());
        c.setNationality(Objects.toString(nationalityCombo.getSelectedItem(), ""));
        if (attempt(() -> controller.save(c))) {
            editing = c;
            Ui.selectRow(table, model, row -> row.getId() == c.getId());
            formTitle.setText("Editing: " + c.getFullName());
            deleteButton.setEnabled(true);
            feedback.setForeground(Theme.success());
            feedback.setText("Saved " + c.getFullName() + ".");
        }
    }

    private void delete() {
        if (editing == null) return;
        Customer c = editing;
        if (Dialogs.confirm(this, "Delete " + c.getFullName() + "?",
                "This guest profile will be permanently removed. This cannot be undone.", "Delete guest", true)) {
            if (attempt(() -> controller.delete(c))) {
                startNew();
            }
        }
    }
}
