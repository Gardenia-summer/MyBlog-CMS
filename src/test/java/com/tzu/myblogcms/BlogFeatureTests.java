package com.tzu.myblogcms;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.article.ArticleForm;
import com.tzu.myblogcms.article.ArticleRepository;
import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.AuthService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.Role;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.auth.User;
import com.tzu.myblogcms.auth.UserRepository;
import com.tzu.myblogcms.category.Category;
import com.tzu.myblogcms.category.CategoryRepository;
import com.tzu.myblogcms.comment.CommentRepository;
import com.tzu.myblogcms.tag.Tag;
import com.tzu.myblogcms.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BlogFeatureTests {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userCanRegisterAndLoginSeparatelyFromAdmin() throws Exception {
        String username = "user_login_case";

        mockMvc.perform(post("/register")
                        .param("username", username)
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(authService.authenticate(username, "admin123", Role.USER)).isNotNull();

        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(post("/admin/login")
                        .param("username", username)
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void adminCanLoginOnlyWithAdminRole() throws Exception {
        User admin = userRepository.save(new User("admin_login_case", passwordEncoder.encode("admin123"), Role.ADMIN));

        mockMvc.perform(post("/admin/login")
                        .param("username", admin.getUsername())
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));
    }

    @Test
    void exampleAdminSqlUsesAdmin123Password() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/data-example.sql"));
        Matcher matcher = Pattern.compile("'(\\$2a\\$[^']+)'").matcher(sql);

        assertThat(matcher.find()).isTrue();
        assertThat(passwordEncoder.matches("admin123", matcher.group(1))).isTrue();
    }

    @Test
    void userCanCreateEditDeleteOwnArticle() {
        User author = userRepository.save(new User("author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Category Own Case"));
        Tag tag = tagRepository.save(new Tag("Tag Own Case"));

        Article article = articleService.createArticle(form("Original Title", "Original Content", category, List.of(tag)), author.getId());

        assertThat(articleService.listMine(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
                .extracting(Article::getTitle)
                .contains("Original Title");

        articleService.updateOwnArticle(article.getId(), form("Updated Title", "Updated Content", category, List.of(tag)), author.getId());

        assertThat(articleService.requireArticle(article.getId()).getTitle()).isEqualTo("Updated Title");

        articleService.deleteOwnArticle(article.getId(), author.getId());

        assertThat(articleRepository.findById(article.getId())).isEmpty();
    }

    @Test
    void publicPagesSupportSearchAndComments() throws Exception {
        User author = userRepository.save(new User("search_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Search Category Case"));
        Tag tag = tagRepository.save(new Tag("Search Tag Case"));
        Article article = articleService.createArticle(
                form("Searchable Spring Title", "Body mentions persistence", category, List.of(tag)),
                author.getId()
        );

        mockMvc.perform(get("/").param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searchable Spring Title")));

        mockMvc.perform(get("/").param("keyword", "search_author_case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searchable Spring Title")));

        mockMvc.perform(post("/articles/" + article.getId() + "/comments")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(author.getId(), author.getUsername(), Role.USER))
                        .param("content", "Nice article"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/articles/" + article.getId()));

        mockMvc.perform(get("/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nice article")));
    }

    @Test
    void protectedPagesRedirectByRole() throws Exception {
        User user = userRepository.save(new User("protected_user_case", passwordEncoder.encode("admin123"), Role.USER));

        mockMvc.perform(get("/me/articles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), Role.USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void adminCanManageTaxonomyAndDeleteAnyArticle() throws Exception {
        User admin = userRepository.save(new User("admin_manage_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        SessionUser sessionUser = new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN);

        mockMvc.perform(post("/admin/categories")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser)
                        .param("name", "Admin Category Case"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        mockMvc.perform(post("/admin/tags")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser)
                        .param("name", "Admin Tag Case"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        Category category = categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(item -> item.getName().equals("Admin Category Case"))
                .findFirst()
                .orElseThrow();
        Tag tag = tagRepository.findAllByOrderByNameAsc().stream()
                .filter(item -> item.getName().equals("Admin Tag Case"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/admin/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser)
                        .param("title", "Admin Managed Article")
                        .param("content", "Admin content")
                        .param("categoryId", category.getId().toString())
                        .param("tagIds", tag.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));

        Article article = articleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> item.getTitle().equals("Admin Managed Article"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/admin/articles/" + article.getId() + "/delete")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));

        assertThat(articleRepository.findById(article.getId())).isEmpty();
    }

    @Test
    void adminCanDeleteComments() throws Exception {
        User admin = userRepository.save(new User("admin_comment_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        User author = userRepository.save(new User("comment_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Comment Category Case"));
        Article article = articleService.createArticle(form("Comment Article", "Content", category, List.of()), author.getId());
        var comment = commentRepository.save(new com.tzu.myblogcms.comment.Comment(article, author, "Delete me"));

        mockMvc.perform(post("/admin/comments/" + comment.getId() + "/delete")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/comments"));

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    private ArticleForm form(String title, String content, Category category, List<Tag> tags) {
        ArticleForm form = new ArticleForm();
        form.setTitle(title);
        form.setContent(content);
        form.setCategoryId(category.getId());
        form.setTagIds(tags.stream().map(Tag::getId).toList());
        return form;
    }
}
