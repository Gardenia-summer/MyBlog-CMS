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
        return toSessionUser(user);
    }

    @Transactional
    public SessionUser updateBio(Long userId, String bio) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only regular users can update bio");
        }
        String cleanBio = bio == null ? "" : bio.trim();
        if (cleanBio.isEmpty()) {
            throw new IllegalArgumentException("简介不能为空");
        }
        if (cleanBio.length() > 300) {
            throw new IllegalArgumentException("简介不能超过 300 字");
        }
        user.updateBio(cleanBio);
        return toSessionUser(user);
    }

    private SessionUser toSessionUser(User user) {
        return new SessionUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getAvatarUrl(), user.getBio());
    }
}
