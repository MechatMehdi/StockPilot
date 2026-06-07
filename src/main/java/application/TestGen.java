package application;

import application.util.QRCodeGenerator;

public class TestGen {
    public static void main(String[] args) {
        System.out.println("Testing QR Code generation...");
        try {
            javafx.application.Platform.startup(() -> {
                System.out.println("JavaFX Platform started.");
                try {
                    var img = QRCodeGenerator.generateQRCode("http://test", 240, 240);
                    System.out.println("Result: " + img);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                javafx.application.Platform.exit();
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
