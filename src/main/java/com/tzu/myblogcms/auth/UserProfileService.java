package com.tzu.myblogcms.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public SessionUser updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only regular users can update nickname");
        }
        String cleanNickname = nickname == null ? "" : nickname.trim();
        if (cleanNickname.isEmpty()) {
            throw new IllegalArgumentException("Nickname is required");
        }
        if (cleanNickname.length() > 80) {
            throw new IllegalArgumentException("Nickname cannot exceed 80 characters");
        }
        user.updateNickname(cleanNickname);
        return new SessionUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getAvatarUrl());
    }
}
