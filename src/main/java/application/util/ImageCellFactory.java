package application.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.util.Objects;

public class ImageCellFactory<T> implements Callback<TableColumn<T, String>, TableCell<T, String>> {

    //Method to initialize the image column
    @Override
    public TableCell<T, String> call(TableColumn<T, String> col) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        Image image;
                        java.io.File file = new java.io.File(item);
                        if (file.exists()) {
                            image = new Image(file.toURI().toString());
                        } else {
                            String resourcePath = item.startsWith("/") ? "/photos" + item : "/photos/" + item;
                            var stream = getClass().getResourceAsStream(resourcePath);
                            if (stream != null) {
                                image = new Image(stream);
                            } else {
                                throw new IllegalArgumentException("Image not found: " + item);
                            }
                        }
                        ImageView imageView = new ImageView(image);
                        imageView.setFitWidth(80);
                        imageView.setFitHeight(80);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);
                        StackPane pane = new StackPane(imageView);
                        pane.setPrefSize(80, 80);
                        setGraphic(pane);
                    } catch (Exception e) {
                        System.err.println("Could not load image: " + item);
                        setGraphic(null);
                    }
                    setText(null);
                }
            }
        };
    }
}
