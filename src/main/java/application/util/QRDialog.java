package application.util;

import application.service.BarcodeServer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class QRDialog {

    private QRDialog() {}

    public static void show(Window owner) {
        String url = BarcodeServer.getScannerUrl();
        WritableImage qrImage = QRCodeGenerator.generateQRCode(url, 240, 240);

        Label title = new Label("Connect Your Phone");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label subtitle = new Label("Scan this QR code with your phone camera\nto open the barcode scanner.");
        subtitle.setStyle("-fx-font-size:13px;-fx-text-fill:#94A3B8;-fx-text-alignment:center;");
        subtitle.setWrapText(true);

        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(240);
        qrView.setFitHeight(240);
        qrView.setStyle("-fx-effect:dropshadow(three-pass-box,rgba(59,130,246,0.4),20,0,0,0);");

        Label urlLabel = new Label(url);
        urlLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#3B82F6;-fx-cursor:hand;-fx-underline:true;");
        urlLabel.setOnMouseClicked(e -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(url);
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            urlLabel.setText("✓ Copied!");
        });

        Label copyHint = new Label("(click URL to copy)");
        copyHint.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color:#1E2D3D;-fx-text-fill:white;"
                + "-fx-border-color:#334155;-fx-border-radius:6px;"
                + "-fx-background-radius:6px;-fx-padding:9px 28px;"
                + "-fx-font-weight:bold;-fx-cursor:hand;");

        VBox content = new VBox(14, title, subtitle, qrView, urlLabel, copyHint, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));
        content.setStyle("-fx-background-color:#1E2D3D;"
                + "-fx-background-radius:14px;"
                + "-fx-border-color:#334155;"
                + "-fx-border-radius:14px;"
                + "-fx-border-width:1px;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.4),30,0,0,10);");

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        Scene dialogScene = new Scene(content);
        dialogScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogScene.getStylesheets().add(QRDialog.class.getResource("/application.css").toExternalForm());
        dialog.setScene(dialogScene);
        closeBtn.setOnAction(e -> dialog.close());
        dialog.show();
    }
}
