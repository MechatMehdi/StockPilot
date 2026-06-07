package application.model;

import java.time.LocalDateTime;

public class StockLog {
    private int id;
    private int productId;
    private String username;
    private ChangeType changeType;
    private int quantityChange;
    private String note;
    private LocalDateTime createdAt;

    public StockLog() {}

    public StockLog(int id, int productId, String username, ChangeType changeType, int quantityChange, String note, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.username = username;
        this.changeType = changeType;
        this.quantityChange = quantityChange;
        this.note = note;
        this.createdAt = createdAt;
    }

    public StockLog(int productId, String username, ChangeType changeType, int quantityChange, String note) {
        this.productId = productId;
        this.username = username;
        this.changeType = changeType;
        this.quantityChange = quantityChange;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getUsername() { return username; }
    public ChangeType getChangeType() { return changeType; }
    public int getQuantityChange() { return quantityChange; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setUsername(String username) { this.username = username; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public void setNote(String note) { this.note = note; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
