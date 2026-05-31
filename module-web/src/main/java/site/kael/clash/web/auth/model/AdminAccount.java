package site.kael.clash.web.auth.model;

import java.time.LocalDateTime;

public class AdminAccount {
    private String username;
    private PasswordHash passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminAccount() {
    }

    public AdminAccount(String username, PasswordHash passwordHash, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(PasswordHash passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
