package application.dao;

import application.model.User;
import application.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO implements UserRepository {

    @Override
    public void createTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'USER', " +
                "created_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                "); ";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating users table: " + e.getMessage());
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    @Override
    public void insert(Connection conn, User user) {
        String checkSql = "SELECT username, email FROM users WHERE username = ? OR email = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, user.getUsername());
            checkStmt.setString(2, user.getEmail());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) return; // Already exists
            }
        } catch (SQLException e) {
            System.err.println("Check failed: " + e.getMessage());
        }

        String sql = "INSERT INTO users (username, name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            // hash if not already hashed (in a real app we'd be more careful)
            stmt.setString(4, user.getPasswordHash().length() == 64 ? user.getPasswordHash() : hashPassword(user.getPasswordHash()));
            stmt.setString(5, user.getRole());

            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("User creation failed: " + e.getMessage());
        }
    }

    @Override
    public void update(Connection conn, User user, String choice, String newValue) {
        String column = choice;
        if (choice.equals("password")) {
            column = "password_hash";
            newValue = hashPassword(newValue);
        }
        
        String sql = "UPDATE users SET " + column + " = ? WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newValue);
            stmt.setString(2, user.getUsername());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
    }

    @Override
    public void delete(Connection conn, User user) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

    @Override
    public List<User> select(Connection conn, List<String> columns, String condition) {
        // Not used securely, kept for interface compliance
        return new ArrayList<>();
    }

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

    @Override
    public Optional<User> findByUsername(Connection conn, String username) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            parseDateTime(rs.getString("created_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(Connection conn, String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            parseDateTime(rs.getString("created_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return Optional.empty();
    }
}
