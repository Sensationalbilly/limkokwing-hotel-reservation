package com.hotel.ui;

import com.hotel.controller.AttendanceController;
import com.hotel.controller.ValidationException;
import com.hotel.model.Attendance;
import com.hotel.model.User;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.ModalCard;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.ui.components.StatusBadge;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * Clock-in card shown to staff at their first sign-in of the day. The arrival time is recorded
 * by the system; staff also type the time they arrived, but lateness is decided by the recorded time.
 */
public class AttendanceDialog extends ModalCard {

    private final AttendanceController controller = new AttendanceController();
    private final Attendance record;
    private final LocalTime recorded;
    private final JSpinner timeSpinner;
    private final JTextArea noteArea = new JTextArea(3, 20);
    private final JLabel message = new JLabel(" ");

    public AttendanceDialog(Window owner, User user, Attendance record) {
        super(owner, greeting(record.getClockIn().toLocalTime()) + ", " + firstName(user.getFullName()),
                "Clock in for " + Formats.longDate(record.getWorkDate()));
        this.record = record;
        this.recorded = record.getClockIn().toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        addHeaderBadge(record.isLate()
                ? new StatusBadge("Late by " + duration(record.getLateMinutes()), Theme.danger())
                : new StatusBadge("On time", Theme.success()));

        // Recorded time: large and read-only
        JLabel timeLabel = Ui.heading(Formats.time(recorded), 30f);
        JLabel caption = Ui.muted("Recorded automatically when you signed in  ·  Freetown time (GMT)");
        JPanel recordedTile = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.background());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        recordedTile.setOpaque(false);
        recordedTile.setBorder(new EmptyBorder(12, 16, 12, 16));
        recordedTile.add(Ui.muted("Your sign-in time"), BorderLayout.NORTH);
        recordedTile.add(timeLabel, BorderLayout.CENTER);
        recordedTile.add(caption, BorderLayout.SOUTH);
        recordedTile.getAccessibleContext().setAccessibleName("Your sign-in time: " + Formats.time(recorded));

        JLabel rules = Ui.label("<html><div style='width:" + Theme.scaled(340) + "px'>Work starts at <b>"
                + Formats.time(AttendanceController.WORK_START) + "</b>. Anyone signing in after <b>"
                + Formats.time(AttendanceController.LATE_AFTER) + "</b> is marked late. Your status is based on "
                + "the recorded time above.</div></html>");

        timeSpinner = new JSpinner(new SpinnerDateModel(toDate(record.getClockIn().truncatedTo(ChronoUnit.MINUTES)),
                null, null, Calendar.MINUTE));
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "h:mm a"));
        timeSpinner.getAccessibleContext().setAccessibleDescription("The time you arrived at work. Use the arrow keys to change it.");

        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(Theme.font(Font.PLAIN, 13f));
        noteArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        noteArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane noteScroll = new JScrollPane(noteArea);
        noteScroll.setBorder(BorderFactory.createLineBorder(Theme.border()));

        FormPanel form = new FormPanel();
        form.addField("What time did you arrive?", KeyEvent.VK_A, timeSpinner);
        form.addField(record.isLate() ? "Reason for being late" : "Note (optional)", KeyEvent.VK_R, noteScroll);

        message.setFont(Theme.font(Font.PLAIN, 13f));
        timeSpinner.addChangeListener(e -> checkTypedTime());

        JPanel body = Ui.stack(16, recordedTile, rules, form, message);
        body.setPreferredSize(new Dimension(Theme.scaled(500), body.getPreferredSize().height + Theme.scaled(30)));
        setBody(body);

        addAction(new BaseButton("Remind me later"), null, true);
        PrimaryButton confirm = addButton(new PrimaryButton("Confirm attendance"), false);
        confirm.setMnemonic(KeyEvent.VK_C);
        confirm.addActionListener(e -> submit());
        setDefaultAction(confirm);
    }

    public void open() {
        showCard();
    }

    /** Live warning while the staff member changes the time (ChangeListener). */
    private void checkTypedTime() {
        long difference = ChronoUnit.MINUTES.between(typedTime(), recorded);
        if (Math.abs(difference) <= AttendanceController.MATCH_TOLERANCE_MINUTES) {
            showMessage(" ", Theme.mutedText());
        } else {
            showMessage("This is " + duration((int) Math.abs(difference)) + (difference > 0 ? " earlier" : " later")
                    + " than your recorded sign-in time (" + Formats.time(recorded) + "). Your attendance is still "
                    + "based on the recorded time, and the administrator will see both.", Theme.warning());
        }
    }

    private void submit() {
        try {
            controller.confirm(record, typedTime(), noteArea.getText());
            close(Boolean.TRUE);
        } catch (ValidationException e) {
            showMessage(e.getMessage(), Theme.danger());
        } catch (Exception e) {
            Dialogs.error(this, e);
        }
    }

    private LocalTime typedTime() {
        return ((Date) timeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault())
                .toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private void showMessage(String text, Color color) {
        message.setForeground(color);
        message.setText("<html><div style='width:" + Theme.scaled(340) + "px'>"
                + text.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
        message.getAccessibleContext().setAccessibleName(text);
        pack();
    }

    private static Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    static String duration(int minutes) {
        if (minutes < 60) return minutes + " min";
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    private static String greeting(LocalTime time) {
        return time.getHour() < 12 ? "Good morning" : time.getHour() < 17 ? "Good afternoon" : "Good evening";
    }

    private static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
