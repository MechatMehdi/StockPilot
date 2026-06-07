package application.config;

import java.io.*;
import java.util.Properties;

/**
 * Singleton that holds user-configurable application settings.
 * Values are persisted to {@code data/settings.properties} so they survive restarts.
 */
public class AppSettings {

    private static final String SETTINGS_FILE = "data/settings.properties";
    private static AppSettings instance;

    // ── Defaults ─────────────────────────────────────────────────────────────
    private String storeName      = "My Store";
    private String storeAddress   = "";
    private String currencySymbol = "";       // intentionally blank — user defines it
    private double taxRate        = 0.0;      // percentage, e.g. 20.0 = 20%
    private int    lowStockDefault = 5;
    private String receiptsDirectory = "";    // If empty, will prompt the user

    // ── Singleton ─────────────────────────────────────────────────────────────
    private AppSettings() { load(); }

    public static AppSettings getInstance() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return;
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            storeName       = props.getProperty("storeName",       storeName);
            storeAddress    = props.getProperty("storeAddress",    storeAddress);
            currencySymbol  = props.getProperty("currencySymbol",  currencySymbol);
            receiptsDirectory = props.getProperty("receiptsDirectory", receiptsDirectory);
            lowStockDefault = Integer.parseInt(props.getProperty("lowStockDefault", String.valueOf(lowStockDefault)));
            taxRate         = Double.parseDouble(props.getProperty("taxRate",        String.valueOf(taxRate)));
        } catch (Exception e) {
            System.err.println("[AppSettings] Failed to load settings: " + e.getMessage());
        }
    }

    public void save() {
        try {
            new File("data").mkdirs();
            Properties props = new Properties();
            props.setProperty("storeName",       storeName);
            props.setProperty("storeAddress",    storeAddress);
            props.setProperty("currencySymbol",  currencySymbol);
            props.setProperty("receiptsDirectory", receiptsDirectory);
            props.setProperty("lowStockDefault", String.valueOf(lowStockDefault));
            props.setProperty("taxRate",         String.valueOf(taxRate));
            try (OutputStream out = new FileOutputStream(SETTINGS_FILE)) {
                props.store(out, "StockPilot Settings");
            }
        } catch (Exception e) {
            System.err.println("[AppSettings] Failed to save settings: " + e.getMessage());
        }
    }

    /** Formats a numeric value with the configured currency symbol prefix. */
    public String formatPrice(double amount) {
        String sym = currencySymbol != null ? currencySymbol.trim() : "";
        return sym.isEmpty()
                ? String.format("%.2f", amount)
                : sym + " " + String.format("%.2f", amount);
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────
    public String getStoreName()      {
        if ("My Store".equals(storeName) && application.model.Session.loggedUser != null) {
            return application.model.Session.loggedUser.getUsername() + " Store";
        }
        return storeName;
    }
    public String getStoreAddress()   { return storeAddress; }
    public String getCurrencySymbol() { return currencySymbol; }
    public double getTaxRate()        { return taxRate; }
    public int    getLowStockDefault(){ return lowStockDefault; }
    public String getReceiptsDirectory() { return receiptsDirectory; }

    public void setStoreName(String v)       { this.storeName      = v; }
    public void setStoreAddress(String v)    { this.storeAddress   = v; }
    public void setCurrencySymbol(String v)  { this.currencySymbol = v; }
    public void setTaxRate(double v)         { this.taxRate        = v; }
    public void setLowStockDefault(int v)    { this.lowStockDefault= v; }
    public void setReceiptsDirectory(String v) { this.receiptsDirectory = v; }
}
