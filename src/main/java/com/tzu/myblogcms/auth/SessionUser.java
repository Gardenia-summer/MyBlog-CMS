package com.tzu.myblogcms.auth;

import java.io.Serializable;

public record SessionUser(Long id, String username, String nickname, Role role, String avatarUrl) implements Serializable {

    public SessionUser(Long id, String username, Role role) {
        this(id, username, username, role, null);
    }

    public SessionUser(Long id, String username, Role role, String avatarUrl) {
        this(id, username, username, role, avatarUrl);
    }
}
