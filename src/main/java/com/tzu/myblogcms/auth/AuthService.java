package com.tzu.myblogcms.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public SessionUser authenticate(String username, String password, Role requiredRole) {
        if (username == null || password == null) {
            return null;
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> requiredRole == null || user.getRole() == requiredRole)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> new SessionUser(user.getId(), user.getUsername(), user.getRole()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return authenticate(username, password, null) != null;
    }

    @Transactional
    public User registerUser(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }
        return userRepository.save(new User(cleanUsername, passwordEncoder.encode(password), Role.USER));
    }
}
