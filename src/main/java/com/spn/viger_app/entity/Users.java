package com.spn.viger_app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng Users (chỉ chứa tài khoản admin).
 * Bảng này lưu thông tin đăng nhập và quản lý của admin hệ thống.
 */
@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên đăng nhập của admin, phải là duy nhất.
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Mật khẩu đã được mã hóa (BCrypt).
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * Họ và tên đầy đủ của admin.
     */
    @Column(name = "full_name", length = 255)
    private String fullName;

    /**
     * Vai trò của người dùng. Mặc định là "ADMIN".
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role = "ADMIN";

    /**
     * Trạng thái tài khoản (true = đang hoạt động).
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Thời điểm tạo tài khoản.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm cập nhật tài khoản gần nhất.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Users() {
    }

    public Users(Long id, String username, String password, String fullName, String role, 
        Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    // ==================== Builder ====================

    public static UsersBuilder builder() {
        return new UsersBuilder();
    }

    public static class UsersBuilder {
        private Long id;
        private String username;
        private String password;
        private String fullName;
        private String role = "ADMIN";
        private Boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        UsersBuilder() {
        }

        public UsersBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UsersBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UsersBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UsersBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UsersBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UsersBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public UsersBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UsersBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Users build() {
            return new Users(id, username, password, fullName, role, active, createdAt, updatedAt);
        }
    }
}
