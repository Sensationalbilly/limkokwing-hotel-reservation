package com.hotel.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Brings an existing database up to date with newer versions of the app, so a database
 * created from an older schema.sql keeps working without being re-created.
 */
public final class SchemaUpdater {

    private SchemaUpdater() { }

    public static void apply() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement()) {
            if (!columnExists(c, "users", "must_change_password")) {
                st.executeUpdate("ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE");
            }
            if (!columnExists(c, "rooms", "amenities")) {
                st.executeUpdate("ALTER TABLE rooms ADD COLUMN amenities VARCHAR(500) NULL");
            }
            if (!columnExists(c, "users", "rating")) {
                st.executeUpdate("ALTER TABLE users ADD COLUMN rating TINYINT NULL");
            }
            st.executeUpdate("CREATE TABLE IF NOT EXISTS attendance ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " user_id INT NOT NULL,"
                    + " work_date DATE NOT NULL,"
                    + " clock_in DATETIME NOT NULL,"
                    + " reported_time TIME NULL,"
                    + " late_minutes INT NOT NULL DEFAULT 0,"
                    + " note VARCHAR(255) DEFAULT '',"
                    + " clock_out DATETIME NULL,"
                    + " UNIQUE KEY uq_attendance_day (user_id, work_date),"
                    + " CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS activity_log ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " user_id INT NULL,"
                    + " action VARCHAR(30) NOT NULL,"
                    + " booking_id INT NULL,"
                    + " amount DECIMAL(10,2) NULL,"
                    + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " INDEX idx_activity_user (user_id, created_at),"
                    + " CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS password_requests ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " user_id INT NOT NULL,"
                    + " note VARCHAR(255) DEFAULT '',"
                    + " status ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',"
                    + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " handled_at TIMESTAMP NULL DEFAULT NULL,"
                    + " handled_by INT NULL,"
                    + " CONSTRAINT fk_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,"
                    + " CONSTRAINT fk_requests_admin FOREIGN KEY (handled_by) REFERENCES users (id) ON DELETE SET NULL)");
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
