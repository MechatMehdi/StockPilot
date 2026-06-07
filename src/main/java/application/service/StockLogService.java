package application.service;

import application.dao.DatabaseConnection;
import application.model.Session;
import application.model.StockLog;
import application.repository.StockLogRepository;

import java.util.List;

/**
 * Service layer for stock-log operations.
 *
 * <p>All public read methods are scoped to the currently authenticated user
 * (via {@link Session#loggedUser}) so that log entries are never leaked
 * across user accounts — consistent with the user-isolation contract
 * already enforced by {@code ProductDAO}.
 */
public class StockLogService {

    private final StockLogRepository logRepo;

    public StockLogService(StockLogRepository repo) {
        this.logRepo = repo;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the username of the currently logged-in user, or {@code "admin"} as fallback. */
    private String currentUsername() {
        return Session.loggedUser != null ? Session.loggedUser.getUsername() : "admin";
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    public void logChange(StockLog log) {
        logRepo.insert(DatabaseConnection.getConnection(), log);
    }

    // -------------------------------------------------------------------------
    // Read — user-scoped (primary API for controllers)
    // -------------------------------------------------------------------------

    /** Returns ALL log entries belonging to the current user, newest first. */
    public List<StockLog> getAllLogs() {
        return logRepo.findAllByUser(DatabaseConnection.getConnection(), currentUsername());
    }

    /** Returns the most-recent {@code limit} log entries for the current user. */
    public List<StockLog> getRecentLogs(int limit) {
        return logRepo.findRecentByUser(DatabaseConnection.getConnection(), limit, currentUsername());
    }

    /**
     * Returns all log entries for a specific product that belong to the
     * current user.  Products are already user-scoped in {@code ProductDAO},
     * so passing an ID that does not belong to the current user will naturally
     * return an empty list.
     */
    public List<StockLog> getLogsByProduct(int productId) {
        return logRepo.findByProductAndUser(DatabaseConnection.getConnection(), productId, currentUsername());
    }

    // -------------------------------------------------------------------------
    // Read — unscoped (reserved for future admin/reporting views)
    // -------------------------------------------------------------------------

    /** Returns ALL log entries across every user. Do NOT expose this to a
     *  regular-user view without an authorization check. */
    public List<StockLog> getAllLogsUnscoped() {
        return logRepo.findAll(DatabaseConnection.getConnection());
    }
}
