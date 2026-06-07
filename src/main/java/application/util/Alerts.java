package application.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;

import java.util.Optional;

public class Alerts {
    /*===============================Messages shown to the user when an action is done===============================*/
    //Information message
    public static void showInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), _ -> alert.close()));
        timeline.play();
    }

    //Warning message
    public static void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), _ -> alert.close()));
        timeline.play();
    }

    //Confirmation message
    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

}
