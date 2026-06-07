package application.controller;

import application.dao.ProductDAO;
import application.model.Product;
import application.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import application.util.ImageCellFactory;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CatalogueController implements BarcodeReceiver {

    @FXML private TableView<Product>          productTable;
    @FXML private TableColumn<Product, String>  imageColumn;
    @FXML private TableColumn<Product, String>  skuColumn;
    @FXML private TableColumn<Product, String>  nameColumn;
    @FXML private TableColumn<Product, String>  categoryColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Double>  priceColumn;
    @FXML private TableColumn<Product, Void>    actionColumn;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    private final ProductService         productService = new ProductService(new ProductDAO());
    private final ObservableList<Product> productList   = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (imageColumn != null) {
            imageColumn.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
            imageColumn.setCellFactory(new ImageCellFactory<>());
        }

        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));

        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn   = new Button("✏  Edit");
            private final Button deleteBtn = new Button("🗑  Delete");
            {
                editBtn.getStyleClass().add("secondary-btn");
                editBtn.setStyle("-fx-padding:5px 12px;-fx-font-size:12px;");
                deleteBtn.getStyleClass().add("danger-btn");
                deleteBtn.setStyle("-fx-padding:5px 12px;-fx-font-size:12px;");
                editBtn.setOnAction(e   -> editProduct(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteProduct(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, editBtn, deleteBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        loadProducts();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                loadProducts();
            } else {
                productList.setAll(productService.searchProductByTitle(newVal));
                if (countLabel != null) {
                    int count = productList.size();
                    countLabel.setText(count + (count == 1 ? " result" : " results"));
                }
            }
        });
    }

    // ── BarcodeReceiver ──────────────────────────────────────────────────────

    /**
     * When the phone scans a barcode while the Catalogue view is active,
     * the search field is populated and the table filters automatically.
     */
    @Override
    public void onBarcodeScanned(String barcode) {
        searchField.setText(barcode);  // triggers the existing textProperty listener
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void loadProducts() {
        productList.setAll(productService.getAll());
        productTable.setItems(productList);
        if (countLabel != null) {
            int count = productList.size();
            countLabel.setText(count + (count == 1 ? " product" : " products"));
        }
    }

    private void editProduct(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/product_form.fxml"));
            Parent root = loader.load();
            ProductFormController controller = loader.getController();
            controller.setProductForEdit(product);

            Stage stage = new Stage();
            stage.setTitle("Edit Product");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setOnHidden(e -> loadProducts());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open edit dialog.", ButtonType.OK).show();
        }
    }

    private void deleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete '" + product.getProductName() + "'?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Product");
        alert.setHeaderText(null);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            productService.archiveProduct(product);
            loadProducts();
        }
    }

    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Catalogue as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("catalogue_export.csv");
        java.io.File file = chooser.showSaveDialog(productTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("SKU,Product Name,Category,Stock,Sell Price,Cost Price,Supplier");
            List<Product> list = productService.getAll();
            for (Product p : list) {
                pw.printf("\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,\"%s\"%n",
                        p.getSku(), p.getProductName(), p.getCategory(),
                        p.getQuantity(), p.getSellPrice(), p.getCostPrice(),
                        p.getSupplier() != null ? p.getSupplier() : "");
            }
            new Alert(Alert.AlertType.INFORMATION, "Exported " + list.size() + " products to:\n" + file.getPath(), ButtonType.OK).show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage(), ButtonType.OK).show();
        }
    }
}
