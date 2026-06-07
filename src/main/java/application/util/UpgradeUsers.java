package application.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpgradeUsers {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/main/database/storeDB.db");
             Statement stmt = conn.createStatement()) {
            int updated = stmt.executeUpdate("UPDATE users SET role = 'ADMIN'");
            System.out.println("Upgraded " + updated + " users to ADMIN.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
