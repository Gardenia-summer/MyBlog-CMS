package com.tzu.myblogcms.auth;

import java.io.Serializable;

public record SessionUser(Long id, String username, Role role) implements Serializable {
}
