package com.tzu.myblogcms.web;

import com.tzu.myblogcms.article.ArticleForm;
import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.category.CategoryService;
import com.tzu.myblogcms.tag.TagService;
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
public class MeArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public MeArticleController(ArticleService articleService, CategoryService categoryService, TagService tagService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @GetMapping("/me/articles")
    public String myArticles(@RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        model.addAttribute("articlePage", articleService.listMine(
                user.id(),
                PageRequest.of(Math.max(page, 0), 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
        model.addAttribute("currentUser", user);
        return "me/articles/list";
    }

    @GetMapping("/me/articles/new")
    public String newArticle(Model model, HttpSession session) {
        prepareForm(model, new ArticleForm());
        model.addAttribute("currentUser", AuthSession.currentUser(session).orElse(null));
        model.addAttribute("action", "/me/articles");
        return "me/articles/form";
    }

    @PostMapping("/me/articles")
    public String create(@ModelAttribute ArticleForm articleForm, HttpSession session, Model model) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        try {
            articleService.createArticle(articleForm, user.id());
            return "redirect:/me/articles";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("currentUser", user);
            model.addAttribute("action", "/me/articles");
            prepareForm(model, articleForm);
            return "me/articles/form";
        }
    }

    @GetMapping("/me/articles/{id}/edit")
    public String edit(@PathVariable Long id, HttpSession session, Model model) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        var article = articleService.requireArticle(id);
        if (!article.getAuthor().getId().equals(user.id())) {
            return "redirect:/me/articles";
        }
        prepareForm(model, articleService.toForm(article));
        model.addAttribute("currentUser", user);
        model.addAttribute("action", "/me/articles/" + id);
        return "me/articles/form";
    }

    @PostMapping("/me/articles/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute ArticleForm articleForm,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        try {
            articleService.updateOwnArticle(id, articleForm, user.id());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/me/articles";
    }

    @PostMapping("/me/articles/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUser user = AuthSession.currentUser(session).orElseThrow();
        try {
            articleService.deleteOwnArticle(id, user.id());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/me/articles";
    }

    private void prepareForm(Model model, ArticleForm articleForm) {
        model.addAttribute("articleForm", articleForm);
        model.addAttribute("categories", categoryService.listCategories());
        model.addAttribute("tags", tagService.listTags());
    }
}
