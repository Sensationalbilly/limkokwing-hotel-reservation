package com.hotel.model;

import java.math.BigDecimal;
import java.time.LocalTime;

/** One staff member's attendance and work figures for a period, with an overall score out of 100. */
public record StaffPerformance(
        User user,
        int daysPresent,
        int onTimeDays,
        int lateDays,
        LocalTime averageArrival,
        int bookings,
        int checkIns,
        int checkOuts,
        int cancellations,
        int guestsAdded,
        BigDecimal revenue,
        int score) {

    public int onTimePercent() {
        return daysPresent == 0 ? 0 : Math.round(onTimeDays * 100f / daysPresent);
    }

    public int workDone() {
        return bookings + checkIns + checkOuts + guestsAdded;
    }
}
