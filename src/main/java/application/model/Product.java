package application.model;

import java.time.LocalDateTime;

public class Product implements CatalogComponent {
    private int id;
    private String sku;
    private String productName;
    private String description;
    private String imagePath;
    private String category;
    private double costPrice;
    private double sellPrice;
    private int quantity;
    private int lowStockThreshold;
    private String supplier;
    private boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(int id, String sku, String productName, String description, String imagePath, String category,
                   double costPrice, double sellPrice, int quantity, int lowStockThreshold, String supplier,
                   boolean isArchived, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sku = sku;
        this.productName = productName;
        this.description = description;
        this.imagePath = imagePath;
        this.category = category;
        this.costPrice = costPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.supplier = supplier;
        this.isArchived = isArchived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isLowStock() {
        return this.quantity <= this.lowStockThreshold;
    }

    // Getters
    public int getId() { return id; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }
    public String getCategory() { return category; }
    public double getCostPrice() { return costPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getQuantity() { return quantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public String getSupplier() { return supplier; }
    public boolean isArchived() { return isArchived; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSku(String sku) { this.sku = sku; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setDescription(String description) { this.description = description; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setCategory(String category) { this.category = category; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String getName() {
        return this.productName;
    }

    @Override
    public double getPrice() {
        return this.sellPrice;
    }
}
