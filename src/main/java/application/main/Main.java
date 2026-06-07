package application.main;

import application.dao.DatabaseConnection;
import application.service.BarcodeServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;

import java.util.Objects;

/**
 * Application entry point.
 *
 * <p>Bootstraps the JavaFX stage with the login scene, registers a
 * shutdown hook to cleanly close the database connection and stop the
 * background barcode server, and sets the app icon from the Stocky mascot.
 */
public class Main extends Application {

    private static final BarcodeServer barcodeServer = new BarcodeServer();

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.closeConnection();
            barcodeServer.stop();
        }, "shutdown-hook"));
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Start background barcode scanning server
        barcodeServer.start();

        // Load initial scene (login)
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("/scenes/login.fxml")));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/application.css")).toExternalForm());

        // ── Stage configuration ────────────────────────────────────────────────
        stage.setTitle("StockPilot — Inventory Management");
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setWidth(1280);
        stage.setHeight(800);

        // Use image.png as the app window icon
        try {
            Image icon = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/photos/image.png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("[Main] Could not load app icon: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        // Enable Windows 11 Native Dark Title Bar
        application.util.DarkModeUtil.setDarkModeForWindow(stage.getTitle());
    }
}
