package com.tzu.myblogcms.web;

import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.AvatarService;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.auth.UserProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final AvatarService avatarService;
    private final UserProfileService userProfileService;

    public ProfileController(AvatarService avatarService, UserProfileService userProfileService) {
        this.avatarService = avatarService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me/profile")
    public String profile(HttpSession session, Model model) {
        model.addAttribute("currentUser", AuthSession.currentUser(session).orElseThrow());
        return "me/profile";
    }

    @PostMapping("/me/profile/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        SessionUser currentUser = AuthSession.currentUser(session).orElseThrow();
        try {
            SessionUser updatedUser = avatarService.updateAvatar(currentUser.id(), avatar);
            session.setAttribute(AuthSession.LOGIN_USER, updatedUser);
            redirectAttributes.addFlashAttribute("message", "头像已更新");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/me/profile";
    }

    @PostMapping("/me/profile/nickname")
    public String updateNickname(@RequestParam("nickname") String nickname,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        SessionUser currentUser = AuthSession.currentUser(session).orElseThrow();
        try {
            SessionUser updatedUser = userProfileService.updateNickname(currentUser.id(), nickname);
            session.setAttribute(AuthSession.LOGIN_USER, updatedUser);
            redirectAttributes.addFlashAttribute("message", "昵称已更新");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/me/profile";
    }
}
