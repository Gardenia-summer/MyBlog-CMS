package com.tzu.myblogcms.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    public static final String DEFAULT_BIO = "这个用户很懒~什么都没有写~";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 300)
    private String bio;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String username, String passwordHash) {
        this(username, passwordHash, Role.USER);
    }

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.nickname = username;
        this.passwordHash = passwordHash;
        this.role = role;
        if (role == Role.USER) {
            this.bio = DEFAULT_BIO;
        }
    }

    @PrePersist
    void prePersist() {
        // 兼容 SQL 种子或迁移遗漏：普通用户入库前补齐公开资料默认值。
        if (this.nickname == null || this.nickname.isBlank()) {
            this.nickname = this.username;
        }
        if (this.role == Role.USER && (this.bio == null || this.bio.isBlank())) {
            this.bio = DEFAULT_BIO;
        }
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
