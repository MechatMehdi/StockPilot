package application.repository;

import application.model.Product;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void createTable(Connection conn);
    void insert(Connection conn, Product product);
    void update(Connection conn, Product product);
    void archive(Connection conn, int productId);
    Optional<Product> findById(Connection conn, int id);
    Optional<Product> findBySku(Connection conn, String sku);
    List<Product> findAll(Connection conn, boolean includeArchived);
    List<Product> search(Connection conn, String keyword);
    List<Product> findByCategory(Connection conn, String category);
    List<Product> findLowStock(Connection conn);
    List<String> findAllCategories(Connection conn);
    int countAll(Connection conn);
    double sumInventoryValue(Connection conn);
    double sumPotentialProfit(Connection conn);
    void updateStock(Connection conn, Product product, int newStock);
}
