package application.dao;

import application.config.AppConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static Connection connection;

    private DatabaseConnection() {}

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(AppConstants.DB_URL);
                // WAL mode prevents "database is locked" errors under concurrent access
                try (Statement pragma = connection.createStatement()) {
                    pragma.execute("PRAGMA journal_mode=WAL");
                    pragma.execute("PRAGMA foreign_keys=ON");
                }
                connection.setAutoCommit(false);
                System.out.println("[DB] Connection established.");
                initializeSchema(connection);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return connection;
    }

    private static void initializeSchema(Connection conn) {
        try {
            new UserDAO().createTable(conn);
            new ProductDAO().createTable(conn);
            new StockLogDAO().createTable(conn);
            conn.commit();
            System.out.println("[DB] Schema initialised.");
        } catch (SQLException e) {
            System.err.println("[DB] Schema init failed: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DB] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Close error: " + e.getMessage());
            }
        }
    }
}
