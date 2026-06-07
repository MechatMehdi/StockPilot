package application.dao;

import application.model.StockLog;
import application.model.ChangeType;
import application.repository.StockLogRepository;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class StockLogDAO implements StockLogRepository {

    // -------------------------------------------------------------------------
    // DDL
    // -------------------------------------------------------------------------

    @Override
    public void createTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS stock_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "product_id INTEGER NOT NULL, " +
                "username TEXT NOT NULL, " +
                "change_type TEXT NOT NULL, " +
                "quantity_change INTEGER NOT NULL DEFAULT 0, " +
                "note TEXT, " +
                "created_at TEXT NOT NULL DEFAULT (datetime('now')), " +
                "FOREIGN KEY (product_id) REFERENCES products(id), " +
                "FOREIGN KEY (username) REFERENCES users(username)" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating stock_log table: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final DateTimeFormatter[] DT_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null) return LocalDateTime.now();
        for (DateTimeFormatter fmt : DT_FORMATS) {
            try { return LocalDateTime.parse(raw, fmt); } catch (DateTimeParseException ignored) {}
        }
        return LocalDateTime.now();
    }

    private StockLog mapRow(ResultSet rs) throws SQLException {
        return new StockLog(
                rs.getInt("id"),
                rs.getInt("product_id"),
                rs.getString("username"),
                ChangeType.valueOf(rs.getString("change_type")),
                rs.getInt("quantity_change"),
                rs.getString("note"),
                parseDateTime(rs.getString("created_at"))
        );
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    @Override
    public void insert(Connection conn, StockLog log) {
        String sql = "INSERT INTO stock_log (product_id, username, change_type, quantity_change, note) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, log.getProductId());
            stmt.setString(2, log.getUsername());
            stmt.setString(3, log.getChangeType().name());
            stmt.setInt(4, log.getQuantityChange());
            stmt.setString(5, log.getNote());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) log.setId(keys.getInt(1));
            }
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error inserting stock log: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Read — unscoped (all users)
    // -------------------------------------------------------------------------

    @Override
    public List<StockLog> findAll(Connection conn) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching all stock logs: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<StockLog> findByProduct(Connection conn, int productId) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log WHERE product_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stock logs for product: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<StockLog> findRecent(Connection conn, int limit) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent stock logs: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Read — user-scoped
    // -------------------------------------------------------------------------

    @Override
    public List<StockLog> findAllByUser(Connection conn, String username) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log WHERE username = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stock logs for user: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<StockLog> findByProductAndUser(Connection conn, int productId, String username) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log WHERE product_id = ? AND username = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stock logs for product+user: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<StockLog> findRecentByUser(Connection conn, int limit, String username) {
        List<StockLog> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_log WHERE username = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent stock logs for user: " + e.getMessage());
        }
        return list;
    }
}
