package application.controller;

import application.config.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML private TextField storeNameField;
    @FXML private TextField storeAddressField;
    @FXML private TextField currencySymbolField;
    @FXML private TextField taxRateField;
    @FXML private Spinner<Integer> lowStockSpinner;
    @FXML private Label feedbackLabel;

    private final AppSettings settings = AppSettings.getInstance();

    @FXML
    public void initialize() {
        lowStockSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, settings.getLowStockDefault()));
        lowStockSpinner.setEditable(true);

        storeNameField.setText(settings.getStoreName());
        storeAddressField.setText(settings.getStoreAddress());
        currencySymbolField.setText(settings.getCurrencySymbol());
        taxRateField.setText(String.valueOf(settings.getTaxRate()));

        feedbackLabel.setVisible(false);
    }

    @FXML
    private void saveSettings() {
        // Validate tax rate
        double tax;
        try {
            tax = Double.parseDouble(taxRateField.getText().trim());
            if (tax < 0 || tax > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFeedback("⚠️  Tax rate must be a number between 0 and 100.", false);
            return;
        }

        // Validate low stock spinner
        int lowStock;
        try {
            String text = lowStockSpinner.getEditor().getText().trim();
            lowStock = Integer.parseInt(text);
            if (lowStock < 1) lowStock = 1;
        } catch (NumberFormatException e) {
            lowStock = settings.getLowStockDefault();
        }

        settings.setStoreName(storeNameField.getText().trim());
        settings.setStoreAddress(storeAddressField.getText().trim());
        settings.setCurrencySymbol(currencySymbolField.getText().trim());
        settings.setTaxRate(tax);
        settings.setLowStockDefault(lowStock);
        settings.save();

        showFeedback("✅  Settings saved successfully.", true);
    }

    private void showFeedback(String msg, boolean success) {
        feedbackLabel.setText(msg);
        feedbackLabel.setVisible(true);
        if (success) {
            feedbackLabel.setStyle(
                    "-fx-background-color: rgba(5,150,105,0.18);"
                    + "-fx-border-color: #059669; -fx-border-radius: 8; -fx-border-width: 1;"
                    + "-fx-text-fill: #34D399; -fx-font-weight: bold;"
                    + "-fx-padding: 10px 16px; -fx-background-radius: 8;");
        } else {
            feedbackLabel.setStyle(
                    "-fx-background-color: rgba(239,68,68,0.15);"
                    + "-fx-border-color: #EF4444; -fx-border-radius: 8; -fx-border-width: 1;"
                    + "-fx-text-fill: #F87171; -fx-font-weight: bold;"
                    + "-fx-padding: 10px 16px; -fx-background-radius: 8;");
        }
    }
}
