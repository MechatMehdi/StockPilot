package application.controller;

import application.dao.StockLogDAO;
import application.model.StockLog;
import application.service.StockLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Activity Log view.
 *
 * <p>Delegates to {@link StockLogService#getAllLogs()} which is now
 * user-scoped: only entries whose {@code username} matches the
 * currently authenticated user are returned.
 */
public class ActivityLogController {

    @FXML private TableView<StockLog> logTable;
    @FXML private TableColumn<StockLog, String>  dateColumn;
    @FXML private TableColumn<StockLog, String>  userColumn;
    @FXML private TableColumn<StockLog, String>  typeColumn;
    @FXML private TableColumn<StockLog, Integer> qtyColumn;
    @FXML private TableColumn<StockLog, String>  noteColumn;

    private final StockLogService logService = new StockLogService(new StockLogDAO());
    private final ObservableList<StockLog> logList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("changeType"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantityChange"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        // getAllLogs() is now user-scoped — no cross-user data leakage.
        logList.setAll(logService.getAllLogs());
        logTable.setItems(logList);
    }

    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Activity Log as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("activity_log.csv");
        java.io.File file = chooser.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Date,User,Action Type,Quantity Change,Notes");
            List<StockLog> list = logList; // export what's in the table
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (StockLog log : list) {
                pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                        log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "",
                        log.getUsername(),
                        log.getChangeType(),
                        log.getQuantityChange(),
                        log.getNote() != null ? log.getNote().replace("\"", "\"\"") : "");
            }
            new Alert(Alert.AlertType.INFORMATION, "Exported " + list.size() + " logs to:\n" + file.getPath(), ButtonType.OK).show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage(), ButtonType.OK).show();
        }
    }
}
