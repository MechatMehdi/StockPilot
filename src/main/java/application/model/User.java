package application.model;

import java.time.LocalDateTime;

public class User {
    private String username;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String name, String email, String passwordHash, String role, LocalDateTime createdAt) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : "USER";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    // For backward compatibility until full refactor
    public User(String username, String name, String email, String password) {
        this(username, name, email, password, "USER", LocalDateTime.now());
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    // Getters
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPassword() { return passwordHash; } // for compatibility
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
