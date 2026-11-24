package com.hotel.ui.components;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** Date picker based on JSpinner that speaks java.time.LocalDate. Arrow keys change the date. */
public class DateField extends JSpinner {

    public DateField(LocalDate initial) {
        super(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        setEditor(new JSpinner.DateEditor(this, "EEE dd MMM yyyy"));
    }

    public LocalDate getDate() {
        return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate date) {
        setValue(toDate(date));
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
