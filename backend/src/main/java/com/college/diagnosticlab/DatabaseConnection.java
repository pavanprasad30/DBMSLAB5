package com.college.diagnosticlab;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL", "db.url");
        String username = setting("DB_USERNAME", "db.username");
        String password = setting("DB_PASSWORD", "db.password");

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Oracle JDBC Thin driver was not found. Build the backend with Maven.", exception);
        }

        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String environmentName, String propertyName) throws SQLException {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName);
        }
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing database configuration. Set DB_URL, DB_USERNAME, and DB_PASSWORD.");
        }
        return value;
    }
}
