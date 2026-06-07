package application.util;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import javafx.event.ActionEvent;
import java.util.function.BiConsumer;

public class ButtonCellFactory<T> implements Callback<TableColumn<T, Void>, TableCell<T, Void>> {
    private final String buttonText;
    private final BiConsumer<T, ActionEvent> onClick;

    //Constructor
    public ButtonCellFactory(String buttonText, BiConsumer<T, ActionEvent> onClick) {
        this.buttonText = buttonText;
        this.onClick = onClick;
    }

    //Method to initialize the button column
    @Override
    public TableCell<T, Void> call(TableColumn<T, Void> col) {
        return new TableCell<>() {
            private final Button btn = new Button(buttonText);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if(empty){
                    setGraphic(null);
                    setText(null);
                } else {
                    btn.setOnAction( e -> {
                        T rowItem = getTableView().getItems().get(getIndex());
                        onClick.accept(rowItem, e);
                    });
                    setGraphic(btn);
                    setText(null);
                }
            }
        };
    }
}
