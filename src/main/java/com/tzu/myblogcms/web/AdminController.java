package com.tzu.myblogcms.web;

import com.tzu.myblogcms.article.ArticleForm;
import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.AuthService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.Role;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.auth.UserAdminService;
import com.tzu.myblogcms.auth.UserRepository;
import com.tzu.myblogcms.category.CategoryService;
import com.tzu.myblogcms.comment.CommentService;
import com.tzu.myblogcms.tag.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final AuthService authService;
    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    public AdminController(AuthService authService,
                           ArticleService articleService,
                           CategoryService categoryService,
                           TagService tagService,
                           CommentService commentService,
                           UserRepository userRepository,
                           UserAdminService userAdminService) {
        this.authService = authService;
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.userRepository = userRepository;
        this.userAdminService = userAdminService;
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/articles";
    }

    @GetMapping("/admin/login")
    public String loginPage(HttpServletRequest request) {
        if (AuthSession.hasRole(request.getSession(false), Role.ADMIN)) {
            return "redirect:/admin/articles";
        }
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        SessionUser user = authService.authenticate(username, password, Role.ADMIN);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Admin username or password is incorrect");
            return "redirect:/admin/login";
        }
        session.setAttribute(AuthSession.LOGIN_USER, user);
        return "redirect:/admin/articles";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    @GetMapping("/admin/articles")
    public String articles(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String keyword,
                           Model model) {
        model.addAttribute("articlePage", articleService.searchArticles(
                keyword,
                null,
                null,
                PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        model.addAttribute("keyword", keyword);
        return "admin/articles/list";
    }

    @GetMapping("/admin/articles/new")
    public String newArticle(Model model) {
        prepareArticleForm(model, new ArticleForm());
        return "admin/articles/form";
    }

    @PostMapping("/admin/articles")
    public String createArticle(@ModelAttribute ArticleForm articleForm, HttpSession session, Model model) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        try {
            articleService.createArticle(articleForm, user.id());
            return "redirect:/admin/articles";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            prepareArticleForm(model, articleForm);
            return "admin/articles/form";
        }
    }

    @PostMapping("/admin/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return "redirect:/admin/articles";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users/list";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        AuthSession.currentUser(session)
                .filter(user -> !user.id().equals(id))
                .ifPresent(user -> userAdminService.deleteUser(id));
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/comments")
    public String comments(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("commentPage", commentService.listComments(
                PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        return "admin/comments/list";
    }

    @PostMapping("/admin/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return "redirect:/admin/comments";
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.listCategories());
        return "admin/categories/list";
    }

    @PostMapping("/admin/categories")
    public String createCategory(@RequestParam String name) {
        categoryService.create(name);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}")
    public String updateCategory(@PathVariable Long id, @RequestParam String name) {
        categoryService.update(id, name);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/tags")
    public String tags(Model model) {
        model.addAttribute("tags", tagService.listTags());
        return "admin/tags/list";
    }

    @PostMapping("/admin/tags")
    public String createTag(@RequestParam String name) {
        tagService.create(name);
        return "redirect:/admin/tags";
    }

    @PostMapping("/admin/tags/{id}")
    public String updateTag(@PathVariable Long id, @RequestParam String name) {
        tagService.update(id, name);
        return "redirect:/admin/tags";
    }

    @PostMapping("/admin/tags/{id}/delete")
    public String deleteTag(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tagService.delete(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/tags";
    }

    private void prepareArticleForm(Model model, ArticleForm articleForm) {
        model.addAttribute("articleForm", articleForm);
        model.addAttribute("categories", categoryService.listCategories());
        model.addAttribute("tags", tagService.listTags());
    }
}
