package application.controller;

/**
 * Implemented by any JavaFX controller that wants to receive barcode data
 * from the mobile phone scanner.
 *
 * <p>The {@link ShellController} registers/unregisters an implementation of
 * this interface with {@link application.service.BarcodeServer} whenever it
 * loads a new view. All callbacks are guaranteed to arrive on the
 * JavaFX Application Thread so implementations can safely touch the UI.</p>
 */
public interface BarcodeReceiver {

    /**
     * Called when the phone scanner successfully reads a barcode or QR code.
     *
     * @param barcode the decoded string value (e.g. an EAN-13, UPC-A, SKU, etc.)
     */
    void onBarcodeScanned(String barcode);
}
