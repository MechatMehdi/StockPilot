package application.repository;

import application.model.User;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void createTable(Connection conn);
    void insert(Connection conn, User user);
    void update(Connection conn, User user, String choice, String newValue);
    void delete(Connection conn, User user);
    List<User> select(Connection conn, List<String> columns, String condition);
    Optional<User> findByUsername(Connection conn, String username);
    Optional<User> findByEmail(Connection conn, String email);
}
