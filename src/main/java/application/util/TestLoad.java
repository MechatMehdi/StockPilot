package application.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.File;

public class TestLoad {
    public static void main(String[] args) {
        Platform.startup(() -> {
            String[] views = {"dashboard", "catalogue", "product_form", "selling", "activity_log", "settings"};
            for (String v : views) {
                try {
                    System.out.println("Loading " + v + "...");
                    Parent root = FXMLLoader.load(TestLoad.class.getResource("/scenes/" + v + ".fxml"));
                    System.out.println("SUCCESS: " + v);
                } catch (Exception e) {
                    System.err.println("FAILED: " + v);
                    e.printStackTrace();
                }
            }
            Platform.exit();
        });
    }
}
