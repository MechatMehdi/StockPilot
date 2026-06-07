package application.repository;

import application.model.StockLog;
import java.sql.Connection;
import java.util.List;

public interface StockLogRepository {
    void createTable(Connection conn);
    void insert(Connection conn, StockLog log);

    /** Returns ALL logs across all users — reserved for admin/super-user use. */
    List<StockLog> findAll(Connection conn);

    /** Returns only logs whose username column matches the given user. */
    List<StockLog> findAllByUser(Connection conn, String username);

    List<StockLog> findByProduct(Connection conn, int productId);

    /** Returns only logs for the given product that belong to the given user. */
    List<StockLog> findByProductAndUser(Connection conn, int productId, String username);

    List<StockLog> findRecent(Connection conn, int limit);

    /** Returns the most-recent {@code limit} logs for the given user. */
    List<StockLog> findRecentByUser(Connection conn, int limit, String username);
}
