package application.service;

public enum UserExistStatus {
    NONE_TAKEN,         // 0 - Username and Email are free
    EMAIL_TAKEN,        // 1
    USERNAME_TAKEN,     // 2
    BOTH_TAKEN          // 3
}