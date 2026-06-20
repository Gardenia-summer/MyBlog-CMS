package com.tzu.myblogcms.web;

import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.article.ArticleLikeService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.Role;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.auth.UserRepository;
import com.tzu.myblogcms.category.CategoryService;
import com.tzu.myblogcms.comment.CommentService;
import com.tzu.myblogcms.tag.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicBlogController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final ArticleLikeService articleLikeService;

    public PublicBlogController(ArticleService articleService,
                                CategoryService categoryService,
                                TagService tagService,
                                CommentService commentService,
                                UserRepository userRepository,
                                ArticleLikeService articleLikeService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.userRepository = userRepository;
        this.articleLikeService = articleLikeService;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) Long tagId,
                        HttpServletRequest request,
                        Model model) {
        int pageIndex = Math.max(page, 0);
        model.addAttribute("articlePage", articleService.searchArticles(
                keyword,
                categoryId,
                tagId,
                PageRequest.of(pageIndex, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        model.addAttribute("categories", categoryService.listCategories());
        model.addAttribute("tags", tagService.listTags());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("tagId", tagId);
        model.addAttribute("currentUser", AuthSession.currentUser(request.getSession(false)).orElse(null));
        return "articles/list";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        var article = articleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // getSession(false) 避免匿名访客只看文章详情时被创建无意义 Session。
        SessionUser currentUser = AuthSession.currentUser(request.getSession(false)).orElse(null);
        model.addAttribute("article", article);
        model.addAttribute("comments", commentService.listByArticle(article));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("likedByCurrentUser", currentUser != null
                && currentUser.role() == Role.USER
                && articleLikeService.isLikedBy(article.getId(), currentUser.id()));
        return "articles/detail";
    }

    @GetMapping("/users/{id}")
    public String userProfile(@PathVariable Long id,
                              @RequestParam(defaultValue = "0") int page,
                              HttpServletRequest request,
                              Model model) {
        var profileUser = userRepository.findById(id)
                .filter(user -> user.getRole() == Role.USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // 公开主页只展示普通用户资料；管理员账号不暴露公开“我的”页面。
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("articlePage", articleService.listByAuthor(
                profileUser.getId(),
                PageRequest.of(Math.max(page, 0), 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        model.addAttribute("currentUser", AuthSession.currentUser(request.getSession(false)).orElse(null));
        return "users/profile";
    }

    @PostMapping("/articles/{id}/comments")
    public String comment(@PathVariable Long id,
                          @RequestParam String content,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        try {
            commentService.create(id, user.id(), content);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/like")
    public String like(@PathVariable Long id, HttpSession session) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        // 点赞接口保持普通表单提交，服务层负责“点赞/取消点赞”的 toggle。
        articleLikeService.toggleLike(id, user.id());
        return "redirect:/articles/" + id;
    }
}
