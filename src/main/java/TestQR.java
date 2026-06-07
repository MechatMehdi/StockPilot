import application.util.QRCodeGenerator;
import javafx.application.Platform;

public class TestQR {
    public static void main(String[] args) {
        System.out.println("Starting test...");
        try {
            Platform.startup(() -> {
                System.out.println("JavaFX Started.");
                try {
                    var img = QRCodeGenerator.generateQRCode("http://test", 240, 240);
                    System.out.println("QR generated: " + (img != null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.exit();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
