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
        // 个人资料只面向普通用户；管理员账号不进入“我的”资料编辑流程。
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
        // 返回新的 SessionUser，让控制器同步 Session，页面顶部和评论署名能立刻显示新昵称。
        return toSessionUser(user);
    }

    @Transactional
    public SessionUser updateBio(Long userId, String bio) {
        User user = userRepository.findById(userId).orElseThrow();
        // 简介属于普通用户公开资料，管理员账号保留为空。
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
        // 简介同样回写到 Session，避免用户保存后仍看到旧资料。
        return toSessionUser(user);
    }

    private SessionUser toSessionUser(User user) {
        return new SessionUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getAvatarUrl(), user.getBio());
    }
}
