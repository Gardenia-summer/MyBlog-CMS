package com.tzu.myblogcms.auth;

import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public final class AuthSession {

    public static final String LOGIN_USER = "LOGIN_USER";

    private AuthSession() {
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(LOGIN_USER) != null;
    }

    public static Optional<SessionUser> currentUser(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(LOGIN_USER);
        if (value instanceof SessionUser sessionUser) {
            return Optional.of(sessionUser);
        }
        return Optional.empty();
    }

    public static boolean hasRole(HttpSession session, Role role) {
        return currentUser(session)
                .map(user -> user.role() == role)
                .orElse(false);
    }
}
