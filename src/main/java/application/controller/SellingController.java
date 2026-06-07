package application.controller;

import application.config.AppSettings;
import application.dao.ProductDAO;
import application.dao.StockLogDAO;
import application.model.ChangeType;
import application.model.Product;
import application.model.Session;
import application.model.StockLog;
import application.service.ProductService;
import application.service.StockLogService;
import application.util.QRDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class SellingController implements BarcodeReceiver {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private Label     latestScanLabel;
    @FXML private TableView<SaleItem> historyTable;
    @FXML private TableColumn<SaleItem, String> timeColumn;
    @FXML private TableColumn<SaleItem, String> productColumn;
    @FXML private TableColumn<SaleItem, String> priceColumn;
    @FXML private TableColumn<SaleItem, String> actionColumn;

    @FXML private TextField  searchField;
    @FXML private ListView<Product> searchResultsList;
    @FXML private Label      subtotalLabel;
    @FXML private Label      taxTextLabel;
    @FXML private Label      taxLabel;
    @FXML private TextField  discountField;
    @FXML private Label      totalLabel;

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProductService  productService = new ProductService(new ProductDAO());
    private final StockLogService logService     = new StockLogService(new StockLogDAO());
    private final AppSettings     settings       = AppSettings.getInstance();

    // ── Session state ─────────────────────────────────────────────────────────
    private final ObservableList<SaleItem>  sessionItems  = FXCollections.observableArrayList();
    private final ObservableList<Product>   searchResults = FXCollections.observableArrayList();
    private double sessionSubtotal = 0.0;
    private double sessionTax      = 0.0;
    private double sessionDiscount = 0.0;
    private double sessionTotal    = 0.0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Inner model for per-session sale items ────────────────────────────────
    /**
     * Lightweight wrapper that keeps the sold {@link Product} and its
     * {@link StockLog} together so we can reverse the operation if the
     * cashier voids a line item.
     */
    public static class SaleItem {
        public final Product  product;
        public final StockLog log;
        public final String   timeStr;

        SaleItem(Product product, StockLog log) {
            this.product = product;
            this.log     = log;
            this.timeStr = log.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    // ── Initialization ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupSearch();

        taxTextLabel.setText(String.format("Tax (%.1f%%):", settings.getTaxRate()));

        discountField.textProperty().addListener((obs, old, val) -> {
            try {
                sessionDiscount = val.trim().isEmpty() ? 0.0 : Double.parseDouble(val.trim());
            } catch (NumberFormatException e) {
                sessionDiscount = 0.0;
            }
            updateTotals();
        });
    }

    private void updateTotals() {
        sessionSubtotal = sessionItems.stream().mapToDouble(i -> i.product.getSellPrice()).sum();
        sessionTax = sessionSubtotal * (settings.getTaxRate() / 100.0);
        sessionTotal = sessionSubtotal + sessionTax - sessionDiscount;
        if (sessionTotal < 0) sessionTotal = 0.0;

        subtotalLabel.setText(settings.formatPrice(sessionSubtotal));
        taxLabel.setText(settings.formatPrice(sessionTax));
        totalLabel.setText(settings.formatPrice(sessionTotal));
    }

    private void setupTable() {
        timeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timeStr));
        productColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().product.getProductName()));
        priceColumn.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f", c.getValue().product.getSellPrice())));

        // ── "Remove" action column ────────────────────────────────────────
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("✕ Void");
            {
                removeBtn.getStyleClass().add("danger-btn");
                removeBtn.setStyle("-fx-padding: 4px 10px; -fx-font-size: 11px;");
                removeBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    voidSaleItem(item);
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        historyTable.setItems(sessionItems);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                searchResults.clear();
            } else {
                searchResults.setAll(productService.searchProductByTitle(newVal.trim()));
            }
        });

        searchResultsList.setItems(searchResults);
        searchResultsList.setCellFactory(param -> new ListCell<>() {
            private final Button sellBtn   = new Button("Sell");
            private final Label  nameLabel = new Label();
            private final Label  stockLabel= new Label();
            private final HBox   layout    = new HBox(10);
            private final VBox   details   = new VBox(2);

            {
                sellBtn.getStyleClass().add("primary-btn");
                sellBtn.setStyle("-fx-padding: 6px 14px; -fx-font-size: 13px; -fx-cursor: hand;");
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                details.getChildren().addAll(nameLabel, stockLabel);
                HBox.setHgrow(details, Priority.ALWAYS);
                layout.setAlignment(Pos.CENTER_LEFT);
                layout.getChildren().addAll(details, sellBtn);
            }

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }
                nameLabel.setText(product.getProductName()
                        + "  ·  " + String.format("%.2f", product.getSellPrice()));

                if (product.getQuantity() > 0) {
                    stockLabel.setText("In stock: " + product.getQuantity() + "  |  SKU: " + product.getSku());
                    stockLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
                    sellBtn.setDisable(false);
                } else {
                    stockLabel.setText("Out of Stock  |  SKU: " + product.getSku());
                    stockLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 12px; -fx-font-weight: bold;");
                    sellBtn.setDisable(true);
                }

                sellBtn.setOnAction(e -> processSale(product, "Manual"));
                setGraphic(layout);
                setStyle("-fx-background-color: transparent;"
                        + "-fx-padding: 12px 8px;"
                        + "-fx-border-color: transparent transparent rgba(59,130,246,0.15) transparent;"
                        + "-fx-border-width: 0 0 1 0;");
            }
        });
    }

    // ── QR pairing ────────────────────────────────────────────────────────────

    @FXML
    private void showQR() {
        QRDialog.show(historyTable.getScene().getWindow());
    }

    // ── BarcodeReceiver ───────────────────────────────────────────────────────

    @Override
    public void onBarcodeScanned(String barcode) {
        Optional<Product> productOpt = productService.findBySku(barcode);
        if (productOpt.isPresent()) {
            processSale(productOpt.get(), "Scanner");
        } else {
            Platform.runLater(() -> {
                latestScanLabel.setText("❌ Unknown Barcode: " + barcode);
                latestScanLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#F87171;");
            });
        }
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    /**
     * Sells one unit of {@code product}, decrements stock in the DB, logs
     * a REMOVE entry, and adds the item to the session table.
     */
    private void processSale(Product product, String source) {
        // Re-fetch from DB to get the freshest stock count
        Product fresh = productService.findBySku(product.getSku()).orElse(product);

        if (fresh.getQuantity() <= 0) {
            Platform.runLater(() -> {
                latestScanLabel.setText("❌ Out of Stock: " + fresh.getProductName());
                latestScanLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#F87171;");
            });
            return;
        }

        fresh.setQuantity(fresh.getQuantity() - 1);
        productService.updateProduct(fresh);

        String   actor = Session.loggedUser != null ? Session.loggedUser.getUsername() : "system";
        StockLog sale  = new StockLog(fresh.getId(), actor, ChangeType.REMOVE, 1,
                fresh.getProductName() + " (Sale via " + source + ")");
        logService.logChange(sale);

        SaleItem saleItem = new SaleItem(fresh, sale);

        Platform.runLater(() -> {
            sessionItems.add(0, saleItem);
            updateTotals();

            latestScanLabel.setText("✅ Sold: " + fresh.getProductName()
                    + "  ·  " + settings.formatPrice(fresh.getSellPrice()));
            latestScanLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#34D399;");

            searchResultsList.refresh(); // stock count changed — redraw search list
        });
    }

    /**
     * Voids a sale line: restores 1 unit of stock to the DB and logs an
     * ADD event to keep the audit trail accurate.
     */
    private void voidSaleItem(SaleItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + item.product.getProductName() + "\" from this sale?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Void Item");
        confirm.setHeaderText(null);
        confirm.showAndWait();
        if (confirm.getResult() != ButtonType.YES) return;

        // Restore stock
        Product fresh = productService.findBySku(item.product.getSku()).orElse(item.product);
        fresh.setQuantity(fresh.getQuantity() + 1);
        productService.updateProduct(fresh);

        // Log the reversal
        String   actor = Session.loggedUser != null ? Session.loggedUser.getUsername() : "system";
        StockLog reversal = new StockLog(fresh.getId(), actor, ChangeType.ADD, 1,
                fresh.getProductName() + " (Sale voided)");
        logService.logChange(reversal);

        // Update UI
        sessionItems.remove(item);
        updateTotals();

        latestScanLabel.setText("↩️  Voided: " + item.product.getProductName());
        latestScanLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#FBBF24;");

        searchResultsList.refresh();
    }

    // ── Session reset ─────────────────────────────────────────────────────────

    @FXML
    private void resetSession() {
        sessionItems.clear();
        discountField.clear();
        sessionDiscount = 0.0;
        updateTotals();
        latestScanLabel.setText("Waiting for barcode scan…");
        latestScanLabel.setStyle("");
        searchField.clear();
    }

    // ── Checkout & Receipt ────────────────────────────────────────────────────

    @FXML
    private void checkout() {
        if (sessionItems.isEmpty()) return;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Checkout");

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #0F172A;");

        Label totalLbl = new Label("Total Due: " + settings.formatPrice(sessionTotal));
        totalLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #34D399;");

        Label tenderLbl = new Label("Amount Tendered:");
        tenderLbl.setStyle("-fx-text-fill: #E2E8F0;");
        TextField tenderField = new TextField();
        tenderField.setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; -fx-font-size: 18px;");

        Label changeLbl = new Label("Change Due: " + settings.formatPrice(0));
        changeLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FBBF24;");

        tenderField.textProperty().addListener((obs, old, val) -> {
            try {
                double tendered = Double.parseDouble(val.trim());
                double change = tendered - sessionTotal;
                changeLbl.setText("Change Due: " + settings.formatPrice(Math.max(0, change)));
            } catch (Exception e) {
                changeLbl.setText("Change Due: " + settings.formatPrice(0));
            }
        });

        Button printBtn = new Button("🖨️ Complete & Print Receipt");
        printBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10px;");
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setOnAction(e -> {
            generateReceipt();
            resetSession();
            stage.close();
        });

        layout.getChildren().addAll(totalLbl, tenderLbl, tenderField, changeLbl, printBtn);
        Scene scene = new Scene(layout, 350, 280);
        stage.setScene(scene);
        stage.show();
    }

    private void generateReceipt() {
        String dirPath = settings.getReceiptsDirectory();
        File dir = null;
        if (dirPath != null && !dirPath.isEmpty()) {
            dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = null;
            }
        }
        
        if (dir == null) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Folder to Save PDF Receipts");
            dir = chooser.showDialog(searchField.getScene().getWindow());
            if (dir != null) {
                settings.setReceiptsDirectory(dir.getAbsolutePath());
                settings.save();
            } else {
                // fallback to data/receipts
                dir = new File("data/receipts");
                dir.mkdirs();
            }
        }

        try {
            String filename = "Receipt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            File file = new File(dir, filename);

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 18);
            Font regularFont = FontFactory.getFont(FontFactory.COURIER, 12);
            Font boldFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 12);

            Paragraph storeName = new Paragraph(settings.getStoreName(), titleFont);
            storeName.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(storeName);

            Paragraph address = new Paragraph(settings.getStoreAddress(), regularFont);
            address.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(address);

            document.add(new Paragraph("========================================", regularFont));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), regularFont));
            document.add(new Paragraph("Cashier: " + (Session.loggedUser != null ? Session.loggedUser.getUsername() : "Staff"), regularFont));
            document.add(new Paragraph("----------------------------------------", regularFont));
            document.add(new Paragraph("QTY  ITEM                        PRICE", boldFont));
            document.add(new Paragraph("----------------------------------------", regularFont));

            for (SaleItem item : sessionItems) {
                String name = item.product.getProductName();
                if (name.length() > 24) name = name.substring(0, 24);
                String price = settings.formatPrice(item.product.getSellPrice());
                document.add(new Paragraph(String.format("1x   %-24s %9s", name, price), regularFont));
            }

            document.add(new Paragraph("----------------------------------------", regularFont));
            document.add(new Paragraph(String.format("Subtotal:                     %10s", settings.formatPrice(sessionSubtotal)), regularFont));
            if (settings.getTaxRate() > 0) {
                document.add(new Paragraph(String.format("Tax (%.1f%%):                  %10s", settings.getTaxRate(), settings.formatPrice(sessionTax)), regularFont));
            }
            if (sessionDiscount > 0) {
                document.add(new Paragraph(String.format("Discount:                    -%10s", settings.formatPrice(sessionDiscount)), regularFont));
            }
            document.add(new Paragraph("========================================", regularFont));
            document.add(new Paragraph(String.format("TOTAL:                        %10s", settings.formatPrice(sessionTotal)), boldFont));
            document.add(new Paragraph("========================================", regularFont));
            
            Paragraph footer1 = new Paragraph("Thank you for your business!", regularFont);
            footer1.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(footer1);
            
            Paragraph footer2 = new Paragraph("Please come again.", regularFont);
            footer2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(footer2);

            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Receipt Saved");
            alert.setHeaderText(null);
            alert.setContentText("PDF Receipt successfully saved to:\n" + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to generate PDF receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String centerText(String text, int width) {
        if (text == null || text.trim().isEmpty()) return "";
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }
}
