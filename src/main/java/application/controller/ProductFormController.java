package application.controller;

import application.dao.ProductDAO;
import application.dao.StockLogDAO;
import application.model.ChangeType;
import application.model.Product;
import application.model.Session;
import application.model.StockLog;
import application.service.ProductService;
import application.service.StockLogService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ProductFormController implements BarcodeReceiver {

    // ── FXML bindings ────────────────────────────────────────────────────────

    @FXML private TextField nameField;
    @FXML private TextField skuField;
    @FXML private TextField categoryField;
    @FXML private TextField costPriceField;
    @FXML private TextField sellPriceField;
    @FXML private TextField quantityField;
    @FXML private TextField thresholdField;
    @FXML private TextField supplierField;
    @FXML private TextArea  descriptionArea;
    @FXML private ImageView imagePreview;
    @FXML private Label     imagePathLabel;

    /** Banner that shows live scan feedback (green = restocked, red = unknown). */
    @FXML private Label     scanFeedbackBar;

    /**
     * Controls how many units are added to an existing product on each scan.
     * Default is 1; the user can change it before scanning.
     */
    @FXML private Spinner<Integer> intakeQtySpinner;

    // ── Services ─────────────────────────────────────────────────────────────

    private final ProductService  productService = new ProductService(new ProductDAO());
    private final StockLogService logService     = new StockLogService(new StockLogDAO());

    // ── State ─────────────────────────────────────────────────────────────────

    private String  savedImagePath = null;
    private Product productToEdit  = null;

    // ────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1);
        intakeQtySpinner.setValueFactory(factory);
        intakeQtySpinner.setEditable(true);
    }

    // ── Scanner pairing ──────────────────────────────────────────────────────

    @FXML
    private void showQR() {
        application.util.QRDialog.show(skuField.getScene().getWindow());
    }

    // ── BarcodeReceiver ──────────────────────────────────────────────────────

    /**
     * Entry point for every barcode received from the phone scanner while
     * this view is active.
     *
     * <p><b>Path A — Known product (restock):</b> If the barcode matches a
     * product already in the database, the stock is incremented by
     * {@code intakeQtySpinner.getValue()} units and persisted immediately.
     * An {@code ADD} stock-log entry is written and the feedback banner turns
     * green. The form fields are NOT modified so the user can keep scanning
     * multiple products in sequence without noise.</p>
     *
     * <p><b>Path B — Unknown product (new entry):</b> The SKU field is
     * populated and the system tries the local DB then the Open Facts API to
     * pre-fill as many fields as possible, leaving the user to review and
     * press "Save".</p>
     *
     * @param barcode raw decoded barcode / QR string from the phone
     */
    @Override
    public void onBarcodeScanned(String barcode) {
        if (skuField.isDisabled()) {
            // We are in "edit a specific product" modal — ignore ambient scans
            showFeedback("ℹ️  Scanner ignored in edit mode.", false);
            return;
        }

        // ── Path A: product already exists → instant restock ────────────────
        Optional<Product> existing = productService.findBySku(barcode);
        if (existing.isPresent()) {
            Product product = existing.get();
            int addQty  = resolveSpinnerValue();
            int newQty  = product.getQuantity() + addQty;
            product.setQuantity(newQty);
            productService.updateProduct(product);

            String actor = Session.loggedUser != null
                    ? Session.loggedUser.getUsername() : "system";
            StockLog log = new StockLog(
                    product.getId(), actor, ChangeType.ADD, addQty,
                    product.getProductName() + " (stock intake via scanner)");
            logService.logChange(log);

            showFeedback(
                    "✅  +" + addQty + "  \"" + product.getProductName()
                            + "\"  →  stock now: " + newQty,
                    true);

            System.out.printf("[ProductForm] Restocked: +%d × '%s' (SKU: %s). New qty: %d%n",
                    addQty, product.getProductName(), barcode, newQty);
            return;
        }

        // ── Path B: unknown product → fill form for manual entry ─────────────
        skuField.setText(barcode);
        showFeedback("🔍  New product — fill in the details and press Save.", false);

        // Layer 2: async online lookup
        nameField.setText("🔍 Looking up product online…");
        nameField.setStyle("-fx-font-style: italic;");

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            application.util.ProductLookupResult result =
                    application.util.ProductLookupService.lookup(barcode);

            javafx.application.Platform.runLater(() -> {
                nameField.setStyle("");
                if (result != null) {
                    nameField.setText(result.name != null ? result.name : "");
                    categoryField.setText(result.category != null ? result.category : "");
                    supplierField.setText(result.brand != null ? result.brand : "");
                    descriptionArea.setText(result.description != null ? result.description : "");

                    if (result.imageUrl != null && !result.imageUrl.isEmpty()) {
                        downloadAndSetImage(result.imageUrl, barcode);
                    }
                    showFeedback("📦  Product found online — review and press Save.", false);
                } else {
                    nameField.setText("");
                    showFeedback("❓  Not found online. Please fill in the details manually.", false);
                }
                nameField.requestFocus();
                nameField.deselect();
            });
        });
    }

    // ── Edit mode setup ──────────────────────────────────────────────────────

    public void setProductForEdit(Product product) {
        this.productToEdit = product;
        nameField.setText(product.getProductName());
        skuField.setText(product.getSku());
        skuField.setDisable(true);
        categoryField.setText(product.getCategory());
        costPriceField.setText(String.valueOf(product.getCostPrice()));
        sellPriceField.setText(String.valueOf(product.getSellPrice()));
        quantityField.setText(String.valueOf(product.getQuantity()));
        thresholdField.setText(String.valueOf(product.getLowStockThreshold()));
        supplierField.setText(product.getSupplier() != null ? product.getSupplier() : "");
        descriptionArea.setText(product.getDescription() != null ? product.getDescription() : "");

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            savedImagePath = product.getImagePath();
            imagePathLabel.setText("Image loaded");
            try {
                imagePreview.setImage(new Image(new File(product.getImagePath()).toURI().toString()));
            } catch (Exception e) {
                System.err.println("[ProductForm] Could not load image: " + e.getMessage());
            }
        }
    }

    // ── Form actions ─────────────────────────────────────────────────────────

    @FXML
    private void handleImagePick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Product Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File chosen = chooser.showOpenDialog(nameField.getScene().getWindow());
        if (chosen != null) {
            try {
                Path imagesDir = Paths.get("data/images");
                Files.createDirectories(imagesDir);
                String ext      = chosen.getName().substring(chosen.getName().lastIndexOf('.'));
                String destName = "product_" + System.currentTimeMillis() + ext;
                Path   dest     = imagesDir.resolve(destName);
                Files.copy(chosen.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                savedImagePath = dest.toString().replace("\\", "/");
                imagePathLabel.setText(destName);
                imagePreview.setImage(new Image(dest.toUri().toString()));
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Could not copy image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String sku  = skuField.getText().trim();
        String cat  = categoryField.getText().trim();

        if (name.isEmpty() || sku.isEmpty() || cat.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Name, SKU, and Category are required fields.");
            return;
        }

        double costPrice, sellPrice;
        int quantity, threshold;
        try {
            costPrice = Double.parseDouble(costPriceField.getText().trim());
            sellPrice = Double.parseDouble(sellPriceField.getText().trim());
            quantity  = Integer.parseInt(quantityField.getText().trim());
            threshold = Integer.parseInt(thresholdField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Prices and quantities must be valid numbers.");
            return;
        }

        if (costPrice < 0 || sellPrice < 0 || quantity < 0 || threshold < 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Prices and quantities cannot be negative.");
            return;
        }

        if (productToEdit == null && productService.findBySku(sku).isPresent()) {
            showAlert(Alert.AlertType.WARNING, "Duplicate SKU",
                    "A product with SKU '" + sku + "' already exists.");
            return;
        }

        Product product = (productToEdit != null) ? productToEdit : new Product();
        product.setProductName(name);
        product.setSku(sku);
        product.setCategory(cat);
        product.setCostPrice(costPrice);
        product.setSellPrice(sellPrice);
        product.setQuantity(quantity);
        product.setLowStockThreshold(threshold);
        product.setSupplier(supplierField.getText().trim());
        product.setDescription(descriptionArea.getText().trim());
        product.setImagePath(savedImagePath);

        if (productToEdit == null) {
            productService.createProduct(product);
        } else {
            productService.updateProduct(product);
        }

        String actor = Session.loggedUser != null ? Session.loggedUser.getUsername() : "system";
        ChangeType cType = (productToEdit == null) ? ChangeType.CREATE : ChangeType.EDIT;
        String note  = (productToEdit == null) ? "Initial product creation" : "Product edited";

        productService.findBySku(sku).ifPresent(saved ->
                logService.logChange(new StockLog(saved.getId(), actor, cType, quantity, note)));

        showAlert(Alert.AlertType.INFORMATION, "Success",
                "Product '" + name + "' saved successfully.");

        if (productToEdit != null) {
            nameField.getScene().getWindow().hide();
        } else {
            clearForm();
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Safely reads the spinner value, falling back to 1 on bad input.
     */
    private int resolveSpinnerValue() {
        try {
            String text = intakeQtySpinner.getEditor().getText().trim();
            int val = Integer.parseInt(text);
            return Math.max(1, Math.min(val, 9999));
        } catch (NumberFormatException e) {
            intakeQtySpinner.getValueFactory().setValue(1);
            return 1;
        }
    }

    /**
     * Updates the scan feedback banner.
     *
     * @param message text to display
     * @param success {@code true} → green success style; {@code false} → neutral info style
     */
    private void showFeedback(String message, boolean success) {
        if (scanFeedbackBar == null) return;
        scanFeedbackBar.setText(message);
        if (success) {
            scanFeedbackBar.setStyle(
                    "-fx-background-color:rgba(5,150,105,0.20);"
                    + "-fx-background-radius:10;"
                    + "-fx-border-color:#059669;"
                    + "-fx-border-radius:10;"
                    + "-fx-border-width:1;"
                    + "-fx-text-fill:#34D399;"
                    + "-fx-font-size:15px;"
                    + "-fx-font-weight:bold;"
                    + "-fx-padding:12px 18px;");
        } else {
            scanFeedbackBar.setStyle(
                    "-fx-background-color:rgba(30,45,61,0.85);"
                    + "-fx-background-radius:10;"
                    + "-fx-border-color:#334155;"
                    + "-fx-border-radius:10;"
                    + "-fx-border-width:1;"
                    + "-fx-text-fill:#94A3B8;"
                    + "-fx-font-size:14px;"
                    + "-fx-padding:12px 18px;");
        }
    }

    private void downloadAndSetImage(String imageUrl, String barcode) {
        try {
            Path imagesDir = Paths.get("data/images");
            Files.createDirectories(imagesDir);
            String ext      = ".jpg";
            String urlLower = imageUrl.toLowerCase();
            if (urlLower.contains(".png"))  ext = ".png";
            else if (urlLower.contains(".webp")) ext = ".webp";
            String fileName = "product_" + barcode.replaceAll("[^a-zA-Z0-9]", "_") + ext;
            Path   dest     = imagesDir.resolve(fileName);

            try (java.io.InputStream in = new java.net.URI(imageUrl).toURL().openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            savedImagePath = dest.toString().replace("\\", "/");
            imagePreview.setImage(new Image(dest.toUri().toString()));
            imagePathLabel.setText("Downloaded (" + fileName + ")");
            System.out.println("[ProductForm] 📷 Image saved to: " + savedImagePath);
        } catch (Exception e) {
            System.err.println("[ProductForm] Could not download product image: " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.clear();
        skuField.clear();
        categoryField.clear();
        costPriceField.clear();
        sellPriceField.clear();
        quantityField.clear();
        thresholdField.clear();
        supplierField.clear();
        descriptionArea.clear();
        imagePathLabel.setText("No image selected");
        imagePreview.setImage(null);
        savedImagePath   = null;
        productToEdit    = null;
        showFeedback("📷  Waiting for barcode scan from phone…", false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
