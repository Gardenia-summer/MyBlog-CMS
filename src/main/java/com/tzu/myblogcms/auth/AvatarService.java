package com.tzu.myblogcms.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarService {

    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    private final UserRepository userRepository;
    private final Path avatarDir;

    public AvatarService(UserRepository userRepository,
                         @Value("${app.upload.avatar-dir:uploads/avatars}") String avatarDir) {
        this.userRepository = userRepository;
        this.avatarDir = Path.of(avatarDir).toAbsolutePath().normalize();
    }

    @Transactional
    public SessionUser updateAvatar(Long userId, MultipartFile avatar) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("管理员不需要上传头像");
        }

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = storeAvatar(user.getId(), avatar);
        user.setAvatarUrl(newAvatarUrl);
        deleteOldAvatar(oldAvatarUrl, newAvatarUrl);
        return new SessionUser(user.getId(), user.getUsername(), user.getRole(), user.getAvatarUrl());
    }

    private String storeAvatar(Long userId, MultipartFile avatar) {
        validate(avatar);
        String fileName = "user-" + userId + "-" + UUID.randomUUID() + extensionFor(avatar.getContentType());
        Path target = avatarDir.resolve(fileName).normalize();
        if (!target.startsWith(avatarDir)) {
            throw new IllegalArgumentException("头像文件名无效");
        }

        try {
            Files.createDirectories(avatarDir);
            try (InputStream inputStream = avatar.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return AVATAR_URL_PREFIX + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("头像保存失败，请稍后再试", ex);
        }
    }

    private void validate(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new IllegalArgumentException("请选择头像文件");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像不能超过 2MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(avatar.getContentType())) {
            throw new IllegalArgumentException("头像只支持 JPG、PNG 或 GIF 格式");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> throw new IllegalArgumentException("头像格式不支持");
        };
    }

    private void deleteOldAvatar(String oldAvatarUrl, String newAvatarUrl) {
        if (oldAvatarUrl == null || oldAvatarUrl.isBlank() || oldAvatarUrl.equals(newAvatarUrl)) {
            return;
        }
        if (!oldAvatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }

        String oldFileName = oldAvatarUrl.substring(AVATAR_URL_PREFIX.length());
        Path oldFile = avatarDir.resolve(oldFileName).normalize();
        if (!oldFile.startsWith(avatarDir)) {
            return;
        }

        try {
            Files.deleteIfExists(oldFile);
        } catch (IOException ignored) {
            // The new avatar is already saved; cleanup can be retried manually if needed.
        }
    }
}
