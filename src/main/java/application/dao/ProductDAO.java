package application.dao;

import application.model.Product;
import application.model.Session;
import application.repository.ProductRepository;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO implements ProductRepository {

    @Override
    public void createTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL DEFAULT 'admin', " +
                "sku TEXT NOT NULL, " +
                "productName TEXT NOT NULL, " +
                "description TEXT, " +
                "imagePath TEXT, " +
                "category TEXT NOT NULL DEFAULT 'Uncategorized', " +
                "costPrice REAL NOT NULL DEFAULT 0.0, " +
                "sellPrice REAL NOT NULL DEFAULT 0.0, " +
                "quantity INTEGER NOT NULL DEFAULT 0, " +
                "lowStockThreshold INTEGER NOT NULL DEFAULT 5, " +
                "supplier TEXT, " +
                "isArchived INTEGER NOT NULL DEFAULT 0, " +
                "createdAt TEXT NOT NULL DEFAULT (datetime('now')), " +
                "updatedAt TEXT NOT NULL DEFAULT (datetime('now')), " +
                "UNIQUE(username, sku)" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN username TEXT DEFAULT 'admin'");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("Error creating products table: " + e.getMessage());
        }
    }

    private String getCurrentUser() {
        return Session.loggedUser != null ? Session.loggedUser.getUsername() : "admin";
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

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("sku"),
                rs.getString("productName"),
                rs.getString("description"),
                rs.getString("imagePath"),
                rs.getString("category"),
                rs.getDouble("costPrice"),
                rs.getDouble("sellPrice"),
                rs.getInt("quantity"),
                rs.getInt("lowStockThreshold"),
                rs.getString("supplier"),
                rs.getInt("isArchived") == 1,
                parseDateTime(rs.getString("createdAt")),
                parseDateTime(rs.getString("updatedAt"))
        );
    }

    @Override
    public void insert(Connection conn, Product product) {
        String sql = "INSERT INTO products (username, sku, productName, description, imagePath, category, costPrice, sellPrice, quantity, lowStockThreshold, supplier) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, getCurrentUser());
            stmt.setString(2, product.getSku());
            stmt.setString(3, product.getProductName());
            stmt.setString(4, product.getDescription());
            stmt.setString(5, product.getImagePath());
            stmt.setString(6, product.getCategory());
            stmt.setDouble(7, product.getCostPrice());
            stmt.setDouble(8, product.getSellPrice());
            stmt.setInt(9, product.getQuantity());
            stmt.setInt(10, product.getLowStockThreshold());
            stmt.setString(11, product.getSupplier());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error inserting product: " + e.getMessage());
        }
    }

    @Override
    public void update(Connection conn, Product product) {
        String sql = "UPDATE products SET sku=?, productName=?, description=?, imagePath=?, category=?, " +
                     "costPrice=?, sellPrice=?, quantity=?, lowStockThreshold=?, supplier=?, isArchived=?, " +
                     "updatedAt=datetime('now') WHERE id=? AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getSku());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getDescription());
            stmt.setString(4, product.getImagePath());
            stmt.setString(5, product.getCategory());
            stmt.setDouble(6, product.getCostPrice());
            stmt.setDouble(7, product.getSellPrice());
            stmt.setInt(8, product.getQuantity());
            stmt.setInt(9, product.getLowStockThreshold());
            stmt.setString(10, product.getSupplier());
            stmt.setInt(11, product.isArchived() ? 1 : 0);
            stmt.setInt(12, product.getId());
            stmt.setString(13, getCurrentUser());
            
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
        }
    }

    @Override
    public void archive(Connection conn, int productId) {
        String sql = "UPDATE products SET isArchived=1, updatedAt=datetime('now') WHERE id=? AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setString(2, getCurrentUser());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error archiving product: " + e.getMessage());
        }
    }

    @Override
    public Optional<Product> findById(Connection conn, int id) {
        String sql = "SELECT * FROM products WHERE id=? AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding product by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Product> findBySku(Connection conn, String sku) {
        String sql = "SELECT * FROM products WHERE sku=? AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            stmt.setString(2, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding product by sku: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAll(Connection conn, boolean includeArchived) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE username=? " + (includeArchived ? "" : "AND isArchived=0");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all products: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Product> search(Connection conn, String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE isArchived=0 AND username=? AND (productName LIKE ? OR sku LIKE ? OR category LIKE ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            stmt.setString(1, getCurrentUser());
            stmt.setString(2, term);
            stmt.setString(3, term);
            stmt.setString(4, term);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching products: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Product> findByCategory(Connection conn, String category) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE isArchived=0 AND username=? AND category=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            stmt.setString(2, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error filtering by category: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Product> findLowStock(Connection conn) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE isArchived=0 AND username=? AND quantity <= lowStockThreshold";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching low stock: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<String> findAllCategories(Connection conn) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE isArchived=0 AND username=? ORDER BY category";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        return list;
    }

    @Override
    public int countAll(Connection conn) {
        String sql = "SELECT COUNT(*) FROM products WHERE isArchived=0 AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting products: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public double sumInventoryValue(Connection conn) {
        String sql = "SELECT SUM(quantity * costPrice) FROM products WHERE isArchived=0 AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error summing value: " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public double sumPotentialProfit(Connection conn) {
        String sql = "SELECT SUM(quantity * (sellPrice - costPrice)) FROM products WHERE isArchived=0 AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getCurrentUser());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error summing profit: " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public void updateStock(Connection conn, Product product, int newStock) {
        String sql = "UPDATE products SET quantity=?, updatedAt=datetime('now') WHERE id=? AND username=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newStock);
            stmt.setInt(2, product.getId());
            stmt.setString(3, getCurrentUser());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
        }
    }
}
