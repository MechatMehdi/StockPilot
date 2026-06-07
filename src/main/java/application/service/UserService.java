package application.service;

import application.dao.DatabaseConnection;
import application.dao.UserDAO;
import application.model.User;
import application.repository.UserRepository;

import java.sql.Connection;
import java.util.Optional;

public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository repo) {
        this.userRepo = repo;
    }

    public User logInUser(String login, String password) {
        Connection conn = DatabaseConnection.getConnection();
        Optional<User> optionalUser = userRepo.findByUsername(conn, login);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String hashedInput = UserDAO.hashPassword(password);
            if (user.getPasswordHash().equals(hashedInput)) {
                return user;
            }
        }
        return null;
    }

    public void addUser(User newUser) {
        userRepo.insert(DatabaseConnection.getConnection(), newUser);
    }

    public UserExistStatus userExist(User newUser) {
        Connection conn = DatabaseConnection.getConnection();
        // Check by username
        Optional<User> byUsername = userRepo.findByUsername(conn, newUser.getUsername());
        // Check by email using a dedicated email lookup
        Optional<User> byEmail = ((UserDAO) userRepo).findByEmail(conn, newUser.getEmail());

        boolean usernameTaken = byUsername.isPresent();
        boolean emailTaken    = byEmail.isPresent();

        if (usernameTaken && emailTaken) return UserExistStatus.BOTH_TAKEN;
        if (usernameTaken)               return UserExistStatus.USERNAME_TAKEN;
        if (emailTaken)                  return UserExistStatus.EMAIL_TAKEN;
        return UserExistStatus.NONE_TAKEN;
    }

    public void updateUser(User user, int choice, String newValue) {
        String[] options = {"username", "name", "email", "password"};
        userRepo.update(DatabaseConnection.getConnection(), user, options[choice], newValue);
    }

    public void deleteUser(User user) {
        userRepo.delete(DatabaseConnection.getConnection(), user);
    }
}
