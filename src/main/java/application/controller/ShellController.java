package application.controller;

import application.model.Session;
import application.service.BarcodeServer;
import application.util.QRCodeGenerator;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

public class ShellController {

    @FXML private StackPane contentArea;
    @FXML private Label     userNameLabel;
    @FXML private Label     userRoleLabel;
    @FXML private Label     avatarLabel;
    @FXML private Canvas    bgCanvas;

    // Admin-only nav items (hidden for USER role)
    @FXML private Button navCatalogue;
    @FXML private Button navAddProduct;
    @FXML private Button navSettings;

    /** Tracks the listener currently registered with BarcodeServer. */
    private Consumer<String> currentBarcodeListener;

    @FXML
    public void initialize() {
        if (Session.loggedUser != null) {
            String name = Session.loggedUser.getName();
            String role = Session.loggedUser.getRole();
            userNameLabel.setText(name);
            userRoleLabel.setText(role != null ? role : "Staff");
            avatarLabel.setText(name != null && !name.isEmpty()
                    ? String.valueOf(name.charAt(0)).toUpperCase() : "U");

            // All navigation buttons are always visible, giving full access to all users.
            if (navCatalogue  != null) {
                navCatalogue.setVisible(true);
                navCatalogue.setManaged(true);
            }
            if (navAddProduct  != null) {
                navAddProduct.setVisible(true);
                navAddProduct.setManaged(true);
            }
            if (navSettings   != null) {
                navSettings.setVisible(true);
                navSettings.setManaged(true);
            }
        }

        // Draw background decorative lines once the node is laid out
        if (bgCanvas != null) {
            StackPane parent = (StackPane) bgCanvas.getParent();
            bgCanvas.widthProperty().bind(parent.widthProperty());
            bgCanvas.heightProperty().bind(parent.heightProperty());
            bgCanvas.widthProperty().addListener((obs, o, n) -> paintBackground(bgCanvas));
            bgCanvas.heightProperty().addListener((obs, o, n) -> paintBackground(bgCanvas));
            paintBackground(bgCanvas);
        }

        loadView("dashboard");
    }

    // ── Sidebar navigation ──────────────────────────────────────────────────
    @FXML private void showDashboard()  { loadView("dashboard"); }
    @FXML private void showCatalogue()  { loadView("catalogue"); }
    @FXML private void showAddProduct() { loadView("product_form"); }
    @FXML private void showActivity()   { loadView("activity_log"); }
    @FXML private void showSettings()   { loadView("settings"); }

    @FXML
    private void showSellingMode() {
        loadView("selling");
    }

    // ── Logout ──────────────────────────────────────────────────────────────
    @FXML
    private void logout(ActionEvent event) {
        // Unregister barcode listener before leaving
        if (currentBarcodeListener != null) {
            BarcodeServer.removeListener(currentBarcodeListener);
            currentBarcodeListener = null;
        }
        Session.loggedUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[ShellController] Logout navigation failed: " + e.getMessage());
        }
    }

    // ── View loading with context-aware barcode routing ──────────────────────
    public void loadView(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/" + viewName + ".fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);

            // Unregister any previous barcode listener
            if (currentBarcodeListener != null) {
                BarcodeServer.removeListener(currentBarcodeListener);
                currentBarcodeListener = null;
            }

            // Register a new listener if the controller implements BarcodeReceiver
            Object controller = loader.getController();
            if (controller instanceof BarcodeReceiver) {
                BarcodeReceiver receiver = (BarcodeReceiver) controller;
                currentBarcodeListener = barcode -> {
                    receiver.onBarcodeScanned(barcode);
                    showScanToast(barcode);
                };
                BarcodeServer.addListener(currentBarcodeListener);
            }
        } catch (IOException e) {
            System.err.println("[ShellController] Failed to load view '" + viewName + "': " + e.getMessage());
        }
    }

    // ── Toast notification ───────────────────────────────────────────────────
    /**
     * Shows a brief "Scanned: {barcode}" toast that slides in from the top
     * of the content area and fades out after 3 seconds.
     */
    private void showScanToast(String barcode) {
        Label toast = new Label("✓  Scanned: " + barcode);
        toast.setStyle(
                "-fx-background-color:linear-gradient(to right,#059669,#10B981);"
                + "-fx-text-fill:white;"
                + "-fx-font-weight:bold;"
                + "-fx-font-size:14px;"
                + "-fx-padding:12px 28px;"
                + "-fx-background-radius:50px;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.40),14,0,0,4);"
        );
        toast.setOpacity(0);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(20, 0, 0, 0));
        contentArea.getChildren().add(toast);

        FadeTransition fadeIn  = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        PauseTransition hold   = new PauseTransition(Duration.seconds(2.5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> contentArea.getChildren().remove(toast));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    // ── Background Rendering ────────────────────────────────────────────────
    private void paintBackground(Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w == 0 || h == 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        drawRoundedLine(gc,
                -w * 0.05, h * 0.20,
                w * 0.85,  h * 1.05,
                w * 0.14,
                Color.web("#1D4ED8", 0.40),
                Color.web("#3B82F6", 0.08));

        drawRoundedLine(gc,
                w * 0.25,  -h * 0.05,
                w * 1.05,  h * 0.75,
                w * 0.10,
                Color.web("#2563EB", 0.30),
                Color.web("#60A5FA", 0.04));
    }

    private void drawRoundedLine(GraphicsContext gc,
                                 double x1, double y1,
                                 double x2, double y2,
                                 double width,
                                 Color startColor,
                                 Color endColor) {
        LinearGradient gradient = new LinearGradient(
                x1, y1, x2, y2,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, startColor),
                new Stop(1.0, endColor)
        );

        gc.setStroke(gradient);
        gc.setLineWidth(width);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(x1, y1, x2, y2);
    }
}
