package application.controller;

import application.dao.ProductDAO;
import application.model.Product;
import application.service.DashboardService;
import application.config.AppSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.Map;

public class DashboardController {

    @FXML private Label totalProductsLabel;
    @FXML private Label inventoryValueLabel;
    @FXML private Label potentialProfitLabel;
    @FXML private Label lowStockLabel;
    
    @FXML private ComboBox<String> earningsPeriodCombo;
    @FXML private Label currentEarningsLabel;
    @FXML private Label previousEarningsLabel;
    
    @FXML private PieChart categoryPieChart;
    @FXML private TableView<Product> lowStockTable;

    private final DashboardService dashboardService = new DashboardService(new ProductDAO());

    @FXML
    public void initialize() {
        try {
            int total = dashboardService.getTotalProducts();
            double value = dashboardService.getTotalInventoryValue();
            double profit = dashboardService.getTotalPotentialProfit();
            int lowStock = dashboardService.getLowStockCount();

            totalProductsLabel.setText(String.valueOf(total));
            inventoryValueLabel.setText(AppSettings.getInstance().formatPrice(value));
            potentialProfitLabel.setText(AppSettings.getInstance().formatPrice(profit));
            lowStockLabel.setText(String.valueOf(lowStock));
            
            earningsPeriodCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
            earningsPeriodCombo.setValue("Daily");
            earningsPeriodCombo.setOnAction(e -> updateEarnings());
            updateEarnings();
            
            loadCharts();
            loadLowStockTable();
        } catch (Exception e) {
            System.err.println("[DashboardController] Error loading stats: " + e.getMessage());
            e.printStackTrace();
            totalProductsLabel.setText("Error");
            inventoryValueLabel.setText("Error");
            potentialProfitLabel.setText("Error");
            lowStockLabel.setText("Error");
        }
    }
    
    private void updateEarnings() {
        if (earningsPeriodCombo.getValue() != null) {
            String period = earningsPeriodCombo.getValue();
            double[] earnings = dashboardService.getEarnings(period);
            currentEarningsLabel.setText(AppSettings.getInstance().formatPrice(earnings[0]));
            previousEarningsLabel.setText("Previous: " + AppSettings.getInstance().formatPrice(earnings[1]));
        }
    }
    
    private void loadCharts() {
        Map<String, Integer> breakdown = dashboardService.getCategoryBreakdown();
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        categoryPieChart.setData(pieChartData);
    }
    
    private void loadLowStockTable() {
        java.util.List<Product> topLowStock = dashboardService.getTopLowStockProducts(5);
        lowStockTable.setItems(FXCollections.observableArrayList(topLowStock));
    }
}
