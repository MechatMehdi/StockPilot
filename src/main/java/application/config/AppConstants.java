package application.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConstants {
    public static final String APP_TITLE = "StockPilot - Inventory Management";
    public static final String IMAGES_DIR;
    public static final String DB_URL;

    // Window settings
    public static final double WINDOW_WIDTH = 1200.0;
    public static final double WINDOW_HEIGHT = 800.0;

    static {
        // Use AppData\Roaming\StockPilot on Windows, ~/.stockpilot on other OS
        String appDataDir;
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            appDataDir = System.getenv("APPDATA") + "\\StockPilot";
        } else {
            appDataDir = System.getProperty("user.home") + "/.stockpilot";
        }

        Path appDir = Paths.get(appDataDir);
        Path dbDir  = appDir.resolve("database");
        Path imgDir = appDir.resolve("images");

        try {
            Files.createDirectories(dbDir);
            Files.createDirectories(imgDir);
        } catch (IOException e) {
            System.err.println("[AppConstants] Could not create app directories: " + e.getMessage());
        }

        DB_URL     = "jdbc:sqlite:" + dbDir.resolve("storeDB.db").toString().replace("\\", "/");
        IMAGES_DIR = imgDir.toString() + System.getProperty("file.separator");
    }
}