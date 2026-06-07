package application.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Embedded HTTP server that:
 *  - Serves a zero-install mobile barcode scanner web page at  GET /scanner
 *  - Exposes a health-check endpoint at                        GET /status
 *  - Receives scanned barcodes at                             POST /scan
 *
 * All barcode events are dispatched on the JavaFX Application Thread via
 * Platform.runLater() so that listener code can safely touch the UI.
 */
public class BarcodeServer {

    private HttpServer server;
    public static final int PORT = 8080;

    /** Thread-safe list of listeners notified on every successful scan. */
    private static final List<Consumer<String>> listeners = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    public void start() {
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
            try (InputStream is = getClass().getResourceAsStream("/keystore.jks")) {
                ks.load(is, "password".toCharArray());
            }
            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "password".toCharArray());
            
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            com.sun.net.httpserver.HttpsServer httpsServer = com.sun.net.httpserver.HttpsServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            httpsServer.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext));
            
            server = httpsServer;
            server.createContext("/scan",    new ScanHandler());
            server.createContext("/scan-image", new ScanImageHandler());
            server.createContext("/scanner", new ScannerPageHandler());
            server.createContext("/status",  new StatusHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.printf("[BarcodeServer] Listening on port %d  |  Scanner URL: %s%n",
                    PORT, getScannerUrl());
        } catch (Exception e) {
            System.err.println("[BarcodeServer] Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[BarcodeServer] Stopped.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Listener management
    // ──────────────────────────────────────────────────────────────────────────

    public static void addListener(Consumer<String> listener) {
        synchronized (listeners) { listeners.add(listener); }
    }

    public static void removeListener(Consumer<String> listener) {
        synchronized (listeners) { listeners.remove(listener); }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Network utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the first non-loopback IPv4 address of this machine so that
     * the phone on the same Wi-Fi network can reach the server.
     */
    public static String getLocalIpAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) {
                return ip;
            }
        } catch (Exception e) {
            System.err.println("[BarcodeServer] UDP method failed: " + e.getMessage());
        }

        // Fallback to iterating interfaces
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                String name = iface.getDisplayName().toLowerCase();
                if (iface.isLoopback() || !iface.isUp() || name.contains("virtual") || name.contains("wsl") || name.contains("vbox") || name.contains("vmware")) {
                    continue;
                }
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            System.err.println("[BarcodeServer] Could not detect local IP: " + e.getMessage());
        }
        return "localhost";
    }

    public static String getScannerUrl() {
        return "https://" + getLocalIpAddress() + ":" + PORT + "/scanner";
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Handlers
    // ──────────────────────────────────────────────────────────────────────────

    /** POST /scan — receives a raw barcode string and dispatches to listeners. */
    private static class ScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (InputStream is = exchange.getRequestBody();
                 java.util.Scanner scanner = new java.util.Scanner(is, StandardCharsets.UTF_8.name())) {
                String barcode = scanner.useDelimiter("\\A").hasNext() ? scanner.next().trim() : "";
                if (!barcode.isEmpty()) {
                    Platform.runLater(() -> {
                        synchronized (listeners) {
                            listeners.forEach(l -> l.accept(barcode));
                        }
                    });
                }
            }

            byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
        }
    }

    /** POST /scan-image — receives a raw image file, decodes using Java ZXing, and dispatches. */
    private static class ScanImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);
                if (image != null) {
                    com.google.zxing.client.j2se.BufferedImageLuminanceSource source = 
                            new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
                    com.google.zxing.BinaryBitmap bitmap = 
                            new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
                    
                    java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class);
                    hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);

                    try {
                        com.google.zxing.Result result = new com.google.zxing.MultiFormatReader().decode(bitmap, hints);
                        String barcode = result.getText();
                        if (barcode != null && !barcode.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                synchronized (listeners) {
                                    listeners.forEach(l -> l.accept(barcode.trim()));
                                }
                            });
                            byte[] response = barcode.trim().getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(200, response.length);
                            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
                            return;
                        }
                    } catch (com.google.zxing.NotFoundException e) {
                        // Barcode not found in image
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] error = "No barcode found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, error.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(error); }
        }
    }

    /** GET /status — health-check so the phone can confirm it reached the PC. */
    private static class StatusHandler implements HttpHandler {
        private static final byte[] BODY =
                "{\"connected\":true,\"app\":\"StockPilot\"}".getBytes(StandardCharsets.UTF_8);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, BODY.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(BODY); }
        }
    }

    /** GET /scanner — serves the self-contained scanner.html page to the phone. */
    private static class ScannerPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            try (InputStream htmlStream = getClass().getResourceAsStream("/web/scanner.html")) {
                if (htmlStream == null) {
                    byte[] msg = "scanner.html not found".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, msg.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                    return;
                }
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = htmlStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] html = buffer.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(html); }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CORS helper
    // ──────────────────────────────────────────────────────────────────────────

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
