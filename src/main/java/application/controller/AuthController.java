package application.controller;

import application.dao.UserDAO;
import application.model.Session;
import application.model.User;
import application.service.UserExistStatus;
import application.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Handles authentication flows (login / register) and the
 * two-blue-line decorative background rendered on a Canvas.
 */
public class AuthController {

    // ── Login fields ────────────────────────────────────────────
    @FXML private TextField     txtLoginUsername;
    @FXML private PasswordField txtLoginPassword;

    // ── Register fields ─────────────────────────────────────────
    @FXML private TextField     txtNewUsername;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    // ── Background canvas (present in both login & register) ────
    @FXML private Canvas bgCanvas;

    private final UserService userService = new UserService(new UserDAO());

    // ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Draw background decorative lines once the node is laid out
        if (bgCanvas != null) {
            // Bind canvas size to the StackPane parent so it fills on resize
            StackPane parent = (StackPane) bgCanvas.getParent();
            bgCanvas.widthProperty().bind(parent.widthProperty());
            bgCanvas.heightProperty().bind(parent.heightProperty());
            bgCanvas.widthProperty().addListener((obs, o, n) -> paintBackground(bgCanvas));
            bgCanvas.heightProperty().addListener((obs, o, n) -> paintBackground(bgCanvas));
            paintBackground(bgCanvas);
        }
    }

    /**
     * Paints two large decorative rounded-cap lines in electric blue
     * across the background — inspired by the Squarespace logo style.
     */
    private void paintBackground(Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w == 0 || h == 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // ── Line 1: diagonal from top-left area sweeping bottom-right ──
        drawRoundedLine(gc,
                -w * 0.05, h * 0.20,   // start X, Y
                w * 0.85,  h * 1.05,   // end X, Y
                w * 0.14,              // stroke width (thick, bold line)
                Color.web("#1D4ED8", 0.40),   // start color
                Color.web("#3B82F6", 0.08));   // end color (fade out)

        // ── Line 2: steeper diagonal, offset to create crossing effect ──
        drawRoundedLine(gc,
                w * 0.25,  -h * 0.05,
                w * 1.05,  h * 0.75,
                w * 0.10,
                Color.web("#2563EB", 0.30),
                Color.web("#60A5FA", 0.04));
    }

    /**
     * Draws a single thick line with round caps and a gradient fade,
     * mimicking the Squarespace-style decorative graphic element.
     *
     * @param gc          GraphicsContext to draw on
     * @param x1,y1       Start point
     * @param x2,y2       End point
     * @param width       Stroke width (the "fatness" of the line)
     * @param startColor  Gradient start color
     * @param endColor    Gradient end color
     */
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

    // ── Login ────────────────────────────────────────────────────
    @FXML
    private void login(ActionEvent event) {
        String username = txtLoginUsername.getText().trim();
        String password = txtLoginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both username and password.");
            return;
        }

        Session.loggedUser = userService.logInUser(username, password);

        if (Session.loggedUser != null) {
            navigateTo(event, "/scenes/shell.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    // ── Register ─────────────────────────────────────────────────
    @FXML
    private void signup(ActionEvent event) {
        String username   = txtNewUsername.getText().trim();
        String password   = txtNewPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Username and password are required.");
            return;
        }

        if (!password.equals(confirmPwd)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Passwords do not match.");
            return;
        }

        User user = new User(username, username, username + "@shop.local", password);
        UserExistStatus status = userService.userExist(user);

        if (status == UserExistStatus.NONE_TAKEN) {
            userService.addUser(user);
            showAlert(Alert.AlertType.INFORMATION, "Account Created", "Your account has been created. Please sign in.");
            navigateTo(event, "/scenes/login.fxml");
        } else {
            showAlert(Alert.AlertType.WARNING, "Already Taken", "That username is already in use.");
        }
    }

    // ── Navigation ───────────────────────────────────────────────
    @FXML private void goToSignup(ActionEvent event) { navigateTo(event, "/scenes/register.fxml"); }
    @FXML private void goToLogin(ActionEvent event)  { navigateTo(event, "/scenes/login.fxml");    }

    /**
     * Navigates to a new scene while preserving the current stage dimensions.
     */
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(
                    getClass().getResource("/application.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[AuthController] Navigation to '" + fxmlPath + "' failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Alert helper ─────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
