package com.hotel.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to MySQL. Settings come from config/db.properties, and
 * environment variables (HOTEL_DB_URL, HOTEL_DB_USER, HOTEL_DB_PASSWORD) override them,
 * so no credentials are hard-coded in the source.
 */
public final class DBConnection {

    private static final Path CONFIG_FILE = Path.of("config", "db.properties");
    private static final Properties CONFIG = loadConfig();

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }

    /** Database URL without query parameters, safe to show on screen. */
    public static String getDisplayUrl() {
        String url = CONFIG.getProperty("db.url");
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query);
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/hotel_reservation"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", "");

        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("Could not read " + CONFIG_FILE + ": " + e.getMessage());
            }
        }

        override(props, "db.url", "HOTEL_DB_URL");
        override(props, "db.user", "HOTEL_DB_USER");
        override(props, "db.password", "HOTEL_DB_PASSWORD");
        return props;
    }

    private static void override(Properties props, String key, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }
}
