package application.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.Map;

/**
 * Utility that generates a QR code as a JavaFX {@link WritableImage}
 * using the ZXing library. The palette is intentionally matched to the
 * StockPilot dark theme so the QR code blends naturally into the UI.
 */
public final class QRCodeGenerator {

    // Dark-blue foreground on near-black background — matches app colour tokens
    private static final int COLOR_DARK  = 0xFF3B82F6; // accent blue  (#3B82F6)
    private static final int COLOR_LIGHT = 0xFF0F1923; // bg-primary   (#0F1923)

    private QRCodeGenerator() { /* utility class — no instantiation */ }

    /**
     * Generates a QR code image for the given {@code text}.
     *
     * @param text   the string to encode (e.g. "http://192.168.1.x:8080/scanner")
     * @param width  desired image width in pixels
     * @param height desired image height in pixels
     * @return a {@link WritableImage} ready to be set on an {@code ImageView},
     *         or {@code null} if encoding failed
     */
    public static WritableImage generateQRCode(String text, int width, int height) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            WritableImage image = new WritableImage(width, height);
            PixelWriter pw = image.getPixelWriter();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pw.setArgb(x, y, matrix.get(x, y) ? COLOR_DARK : COLOR_LIGHT);
                }
            }
            return image;

        } catch (WriterException e) {
            System.err.println("[QRCodeGenerator] Failed to encode QR code: " + e.getMessage());
            return null;
        }
    }
}
