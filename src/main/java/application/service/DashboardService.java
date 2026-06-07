package application.service;

import application.dao.DatabaseConnection;
import application.repository.ProductRepository;
import application.model.Product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final ProductRepository productRepo;

    public DashboardService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public int getTotalProducts() {
        return productRepo.countAll(DatabaseConnection.getConnection());
    }

    public double getTotalInventoryValue() {
        return productRepo.sumInventoryValue(DatabaseConnection.getConnection());
    }

    public double getTotalPotentialProfit() {
        return productRepo.sumPotentialProfit(DatabaseConnection.getConnection());
    }

    public int getLowStockCount() {
        return productRepo.findLowStock(DatabaseConnection.getConnection()).size();
    }

    public Map<String, Integer> getCategoryBreakdown() {
        Map<String, Integer> breakdown = new HashMap<>();
        List<Product> products = productRepo.findAll(DatabaseConnection.getConnection(), false);
        for (Product p : products) {
            breakdown.put(p.getCategory(), breakdown.getOrDefault(p.getCategory(), 0) + 1);
        }
        return breakdown;
    }

    public List<Product> getTopLowStockProducts(int limit) {
        List<Product> lowStock = productRepo.findLowStock(DatabaseConnection.getConnection());
        lowStock.sort(Comparator.comparingInt(Product::getQuantity));
        if (lowStock.size() > limit) {
            return lowStock.subList(0, limit);
        }
        return lowStock;
    }

    public double[] getEarnings(String period) {
        String currentUser = application.model.Session.loggedUser != null ? application.model.Session.loggedUser.getUsername() : "admin";
        double currentEarnings = 0.0;
        double previousEarnings = 0.0;

        String dateModifierCurrent = "";
        String dateModifierPreviousStart = "";
        String dateModifierPreviousEnd = "";

        switch (period.toLowerCase()) {
            case "weekly":
                dateModifierCurrent = "'-7 days'";
                dateModifierPreviousStart = "'-14 days'";
                dateModifierPreviousEnd = "'-7 days'";
                break;
            case "monthly":
                dateModifierCurrent = "'-1 month'";
                dateModifierPreviousStart = "'-2 months'";
                dateModifierPreviousEnd = "'-1 month'";
                break;
            case "daily":
            default:
                dateModifierCurrent = "'-1 day'";
                dateModifierPreviousStart = "'-2 days'";
                dateModifierPreviousEnd = "'-1 day'";
                break;
        }

        String sqlCurrent = "SELECT SUM(s.quantity_change * p.sellPrice) " +
                "FROM stock_log s JOIN products p ON s.product_id = p.id " +
                "WHERE s.change_type = 'REMOVE' AND s.username = ? " +
                "AND s.created_at >= datetime('now', " + dateModifierCurrent + ")";

        String sqlPrevious = "SELECT SUM(s.quantity_change * p.sellPrice) " +
                "FROM stock_log s JOIN products p ON s.product_id = p.id " +
                "WHERE s.change_type = 'REMOVE' AND s.username = ? " +
                "AND s.created_at >= datetime('now', " + dateModifierPreviousStart + ") " +
                "AND s.created_at < datetime('now', " + dateModifierPreviousEnd + ")";

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmtCur = conn.prepareStatement(sqlCurrent);
             java.sql.PreparedStatement stmtPrev = conn.prepareStatement(sqlPrevious)) {
            
            stmtCur.setString(1, currentUser);
            try (java.sql.ResultSet rs = stmtCur.executeQuery()) {
                if (rs.next()) currentEarnings = rs.getDouble(1);
            }

            stmtPrev.setString(1, currentUser);
            try (java.sql.ResultSet rs = stmtPrev.executeQuery()) {
                if (rs.next()) previousEarnings = rs.getDouble(1);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error calculating earnings: " + e.getMessage());
        }

        return new double[]{currentEarnings, previousEarnings};
    }
}
