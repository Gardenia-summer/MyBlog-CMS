package com.tzu.myblogcms.web;

import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.SessionUser;
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

    public PublicBlogController(ArticleService articleService,
                                CategoryService categoryService,
                                TagService tagService,
                                CommentService commentService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
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
        model.addAttribute("article", article);
        model.addAttribute("comments", commentService.listByArticle(article));
        model.addAttribute("currentUser", AuthSession.currentUser(request.getSession(false)).orElse(null));
        return "articles/detail";
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
}
