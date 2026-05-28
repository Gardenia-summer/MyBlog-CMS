package com.tzu.myblogcms.auth;

import java.io.Serializable;

public record SessionUser(Long id, String username, String nickname, Role role, String avatarUrl, String bio) implements Serializable {

    public SessionUser(Long id, String username, Role role) {
        this(id, username, username, role, null, defaultBioFor(role));
    }

    public SessionUser(Long id, String username, Role role, String avatarUrl) {
        this(id, username, username, role, avatarUrl, defaultBioFor(role));
    }

    public SessionUser(Long id, String username, String nickname, Role role, String avatarUrl) {
        this(id, username, nickname, role, avatarUrl, defaultBioFor(role));
    }

    private static String defaultBioFor(Role role) {
        return role == Role.USER ? User.DEFAULT_BIO : null;
    }
}
