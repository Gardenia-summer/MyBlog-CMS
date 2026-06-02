package com.tzu.myblogcms;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.article.ArticleForm;
import com.tzu.myblogcms.article.ArticleLikeRepository;
import com.tzu.myblogcms.article.ArticleLikeService;
import com.tzu.myblogcms.article.ArticleRepository;
import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.AuthService;
import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.AvatarService;
import com.tzu.myblogcms.auth.Role;
import com.tzu.myblogcms.auth.SessionUser;
import com.tzu.myblogcms.auth.User;
import com.tzu.myblogcms.auth.UserAdminService;
import com.tzu.myblogcms.auth.UserProfileService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
    private ArticleLikeRepository articleLikeRepository;

    @Autowired
    private ArticleLikeService articleLikeService;

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
    private AvatarService avatarService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserAdminService userAdminService;

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
        User registeredUser = userRepository.findByUsername(username).orElseThrow();
        assertThat(registeredUser.getNickname()).isEqualTo(username);
        assertThat(registeredUser.getBio()).isEqualTo(User.DEFAULT_BIO);

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
        assertThat(admin.getBio()).isNull();

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
        Category otherCategory = categoryRepository.save(new Category("Other Search Category Case"));
        Tag otherTag = tagRepository.save(new Tag("Other Search Tag Case"));
        Article article = articleService.createArticle(
                form("Searchable Spring Title", "Body mentions persistence", category, List.of(tag)),
                author.getId()
        );
        articleService.createArticle(
                form("Filtered Out Search Article", "Separate body", otherCategory, List.of(otherTag)),
                author.getId()
        );

        String homeHtml = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(homeHtml)
                .contains("class=\"home-search-form\"")
                .contains("name=\"keyword\"")
                .contains("name=\"categoryId\"")
                .contains("name=\"tagId\"")
                .contains("class=\"filter-menu\"")
                .doesNotContain("class=\"toolbar\"");

        mockMvc.perform(get("/").param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searchable Spring Title")));

        mockMvc.perform(get("/").param("keyword", "search_author_case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searchable Spring Title")));

        String categoryHtml = mockMvc.perform(get("/")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(categoryHtml)
                .contains("Searchable Spring Title")
                .doesNotContain("Filtered Out Search Article");

        String tagHtml = mockMvc.perform(get("/")
                        .param("tagId", tag.getId().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(tagHtml)
                .contains("Searchable Spring Title")
                .doesNotContain("Filtered Out Search Article");

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
    void articleTagsRenderInStableNameOrder() throws Exception {
        User author = userRepository.save(new User("stable_tag_order_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Stable Tag Order Category Case"));
        Tag zulu = tagRepository.save(new Tag("Zulu Stable Tag Order Case"));
        Tag alpha = tagRepository.save(new Tag("Alpha Stable Tag Order Case"));
        Article article = articleService.createArticle(
                form("Stable Tag Order Article", "Content", category, List.of(zulu, alpha)),
                author.getId()
        );

        String detailHtml = mockMvc.perform(get("/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(detailHtml.indexOf("Alpha Stable Tag Order Case"))
                .isLessThan(detailHtml.indexOf("Zulu Stable Tag Order Case"));

        String homeHtml = mockMvc.perform(get("/").param("keyword", "Stable Tag Order Article"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int articleIndex = homeHtml.indexOf("Stable Tag Order Article");
        int alphaIndex = homeHtml.indexOf("Alpha Stable Tag Order Case", articleIndex);
        int zuluIndex = homeHtml.indexOf("Zulu Stable Tag Order Case", articleIndex);
        assertThat(articleIndex).isNotNegative();
        assertThat(alphaIndex).isLessThan(zuluIndex);
    }

    @Test
    void userCanLikeAndUnlikeArticle() throws Exception {
        User author = userRepository.save(new User("like_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User liker = userRepository.save(new User("like_user_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Like Category Case"));
        Article article = articleService.createArticle(form("Like Toggle Article", "Like body", category, List.of()), author.getId());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER, new SessionUser(liker.getId(), liker.getUsername(), Role.USER));

        mockMvc.perform(post("/articles/" + article.getId() + "/like").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/articles/" + article.getId()));

        assertThat(articleLikeRepository.countByArticle_Id(article.getId())).isEqualTo(1);
        assertThat(articleLikeRepository.countByUser_Id(liker.getId())).isEqualTo(1);
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getLikeCount()).isEqualTo(1);

        String likedHtml = mockMvc.perform(get("/articles/" + article.getId()).session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(likedHtml)
                .contains("aria-pressed=\"true\"")
                .contains("&#128077;")
                .contains(">1</span>");

        mockMvc.perform(post("/articles/" + article.getId() + "/like").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/articles/" + article.getId()));

        assertThat(articleLikeRepository.countByArticle_Id(article.getId())).isZero();
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getLikeCount()).isZero();
    }

    @Test
    void anonymousAndAdminUsersCannotLikeArticles() throws Exception {
        User author = userRepository.save(new User("like_access_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User admin = userRepository.save(new User("like_access_admin_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        Category category = categoryRepository.save(new Category("Like Access Category Case"));
        Article article = articleService.createArticle(form("Like Access Article", "Like access body", category, List.of()), author.getId());

        mockMvc.perform(post("/articles/" + article.getId() + "/like"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/articles/" + article.getId() + "/like")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));

        String adminDetailHtml = mockMvc.perform(get("/articles/" + article.getId())
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(articleRepository.findById(article.getId()).orElseThrow().getLikeCount()).isZero();
        assertThat(adminDetailHtml)
                .contains("like-detail")
                .contains("&#128077;")
                .doesNotContain("action=\"/articles/" + article.getId() + "/like\"");
    }

    @Test
    void publicListsShowLikeCountsAndSortByLikesThenOldestCreationTime() throws Exception {
        User author = userRepository.save(new User("like_sort_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User likerOne = userRepository.save(new User("like_sort_user_one_case", passwordEncoder.encode("admin123"), Role.USER));
        User likerTwo = userRepository.save(new User("like_sort_user_two_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Like Sort Category Case"));
        Tag tag = tagRepository.save(new Tag("Like Sort Tag Case"));
        Article early = articleService.createArticle(form("Early One Like Article", "Early body", category, List.of(tag)), author.getId());
        Thread.sleep(5);
        Article later = articleService.createArticle(form("Later One Like Article", "Later body", category, List.of(tag)), author.getId());
        Thread.sleep(5);
        Article popular = articleService.createArticle(form("Popular Two Likes Article", "Popular body", category, List.of(tag)), author.getId());

        articleLikeService.toggleLike(early.getId(), likerOne.getId());
        articleLikeService.toggleLike(later.getId(), likerOne.getId());
        articleLikeService.toggleLike(popular.getId(), likerOne.getId());
        articleLikeService.toggleLike(popular.getId(), likerTwo.getId());

        String homeHtml = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(homeHtml)
                .contains("&#128077;")
                .contains(">2</span>")
                .contains(">1</span>");
        assertThat(homeHtml.indexOf("Popular Two Likes Article")).isLessThan(homeHtml.indexOf("Early One Like Article"));
        assertThat(homeHtml.indexOf("Early One Like Article")).isLessThan(homeHtml.indexOf("Later One Like Article"));

        String profileHtml = mockMvc.perform(get("/users/" + author.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(profileHtml.indexOf("Popular Two Likes Article")).isLessThan(profileHtml.indexOf("Early One Like Article"));
        assertThat(profileHtml.indexOf("Early One Like Article")).isLessThan(profileHtml.indexOf("Later One Like Article"));
    }

    @Test
    void deletingUsersOrArticlesCleansUpLikes() {
        User author = userRepository.save(new User("like_cleanup_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User liker = userRepository.save(new User("like_cleanup_user_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Like Cleanup Category Case"));
        Article article = articleService.createArticle(form("Like Cleanup Article", "Cleanup body", category, List.of()), author.getId());
        articleLikeService.toggleLike(article.getId(), liker.getId());

        userAdminService.deleteUser(liker.getId());

        assertThat(articleLikeRepository.countByUser_Id(liker.getId())).isZero();
        assertThat(articleLikeRepository.countByArticle_Id(article.getId())).isZero();
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getLikeCount()).isZero();

        User secondLiker = userRepository.save(new User("like_article_cleanup_user_case", passwordEncoder.encode("admin123"), Role.USER));
        articleLikeService.toggleLike(article.getId(), secondLiker.getId());
        assertThat(articleLikeRepository.countByArticle_Id(article.getId())).isEqualTo(1);

        articleService.deleteArticle(article.getId());

        assertThat(articleRepository.findById(article.getId())).isEmpty();
        assertThat(articleLikeRepository.countByArticle_Id(article.getId())).isZero();
    }

    @Test
    void userCanUpdateNicknameWithoutChangingLoginUsername() throws Exception {
        User user = userRepository.save(new User("nickname_login_case", passwordEncoder.encode("admin123"), Role.USER));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), Role.USER));

        mockMvc.perform(post("/me/profile/nickname")
                        .session(session)
                        .param("nickname", "New Display Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getUsername()).isEqualTo("nickname_login_case");
        assertThat(savedUser.getNickname()).isEqualTo("New Display Name");
        assertThat(authService.authenticate("nickname_login_case", "admin123", Role.USER)).isNotNull();

        SessionUser sessionUser = (SessionUser) session.getAttribute(AuthSession.LOGIN_USER);
        assertThat(sessionUser.username()).isEqualTo("nickname_login_case");
        assertThat(sessionUser.nickname()).isEqualTo("New Display Name");
        assertThat(sessionUser.bio()).isEqualTo(User.DEFAULT_BIO);
    }

    @Test
    void emptyNicknameDoesNotReplaceExistingNickname() throws Exception {
        User user = new User("empty_nickname_case", passwordEncoder.encode("admin123"), Role.USER);
        user.updateNickname("Original Nickname");
        user = userRepository.save(user);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), user.getNickname(), Role.USER, null));

        mockMvc.perform(post("/me/profile/nickname")
                        .session(session)
                        .param("nickname", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getNickname()).isEqualTo("Original Nickname");
        assertThat(((SessionUser) session.getAttribute(AuthSession.LOGIN_USER)).nickname()).isEqualTo("Original Nickname");
    }

    @Test
    void userCanViewAndUpdateBioOnProfile() throws Exception {
        User user = userRepository.save(new User("profile_bio_case", passwordEncoder.encode("admin123"), Role.USER));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER,
                new SessionUser(user.getId(), user.getUsername(), user.getNickname(), Role.USER, null, user.getBio()));

        mockMvc.perform(get("/me/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(User.DEFAULT_BIO)))
                .andExpect(content().string(containsString("action=\"/me/profile/bio\"")));

        mockMvc.perform(post("/me/profile/bio")
                        .session(session)
                        .param("bio", "  我正在认真写博客  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getBio()).isEqualTo("我正在认真写博客");

        SessionUser sessionUser = (SessionUser) session.getAttribute(AuthSession.LOGIN_USER);
        assertThat(sessionUser.bio()).isEqualTo("我正在认真写博客");
        assertThat(sessionUser.nickname()).isEqualTo(user.getNickname());
    }

    @Test
    void invalidBioDoesNotReplaceExistingBio() throws Exception {
        User user = new User("invalid_bio_case", passwordEncoder.encode("admin123"), Role.USER);
        user.updateBio("Original Bio");
        user = userRepository.save(user);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER,
                new SessionUser(user.getId(), user.getUsername(), user.getNickname(), Role.USER, null, user.getBio()));

        mockMvc.perform(post("/me/profile/bio")
                        .session(session)
                        .param("bio", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getBio()).isEqualTo("Original Bio");
        assertThat(((SessionUser) session.getAttribute(AuthSession.LOGIN_USER)).bio()).isEqualTo("Original Bio");

        String tooLongBio = "a".repeat(301);
        mockMvc.perform(post("/me/profile/bio")
                        .session(session)
                        .param("bio", tooLongBio))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getBio()).isEqualTo("Original Bio");
        assertThat(((SessionUser) session.getAttribute(AuthSession.LOGIN_USER)).bio()).isEqualTo("Original Bio");
    }

    @Test
    void publicAuthorAndCommentsUseNicknameInsteadOfUsername() throws Exception {
        User author = new User("hidden_author_username_case", passwordEncoder.encode("admin123"), Role.USER);
        author.updateNickname("Visible Author Nickname");
        author = userRepository.save(author);
        User commenter = new User("hidden_comment_username_case", passwordEncoder.encode("admin123"), Role.USER);
        commenter.updateNickname("Visible Comment Nickname");
        commenter = userRepository.save(commenter);
        Category category = categoryRepository.save(new Category("Nickname Category Case"));
        Article article = articleService.createArticle(
                form("Nickname Display Article", "Nickname body", category, List.of()),
                author.getId()
        );
        commentRepository.save(new com.tzu.myblogcms.comment.Comment(article, commenter, "Nickname comment"));

        String homeHtml = mockMvc.perform(get("/").param("keyword", "Nickname Display Article"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(homeHtml)
                .contains("Visible Author Nickname")
                .contains("href=\"/users/" + author.getId() + "\"")
                .contains("class=\"article-item clickable-article\"")
                .contains("data-href=\"/articles/" + article.getId() + "\"")
                .contains("role=\"link\"")
                .contains("tabindex=\"0\"")
                .contains("article-list.js")
                .doesNotContain("hidden_author_username_case");

        String detailHtml = mockMvc.perform(get("/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(detailHtml)
                .contains("Visible Author Nickname")
                .contains("Visible Comment Nickname")
                .contains("href=\"/users/" + author.getId() + "\"")
                .contains("href=\"/users/" + commenter.getId() + "\"")
                .doesNotContain("hidden_author_username_case")
                .doesNotContain("hidden_comment_username_case");
    }

    @Test
    void publicUserProfileShowsReadOnlyProfileAndArticlesWithoutSession() throws Exception {
        User profileUser = new User("public_profile_username_case", passwordEncoder.encode("admin123"), Role.USER);
        profileUser.updateNickname("Public Profile Nickname");
        profileUser.setAvatarUrl("/uploads/avatars/public-profile.png");
        profileUser.updateBio("Public profile bio");
        profileUser = userRepository.save(profileUser);
        User otherUser = userRepository.save(new User("other_public_profile_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Public Profile Category Case"));
        Tag tag = tagRepository.save(new Tag("Public Profile Tag Case"));
        articleService.createArticle(form("Public Profile Article", "Profile content", category, List.of(tag)), profileUser.getId());
        articleService.createArticle(form("Other Profile Article", "Other content", category, List.of()), otherUser.getId());

        var result = mockMvc.perform(get("/users/" + profileUser.getId()))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(html)
                .contains("Public Profile Nickname")
                .contains("/uploads/avatars/public-profile.png")
                .contains("Public profile bio")
                .contains("Public Profile Article")
                .contains("class=\"article-item clickable-article\"")
                .contains("data-href=\"/articles/")
                .contains("role=\"link\"")
                .contains("tabindex=\"0\"")
                .contains("article-list.js")
                .contains("Public Profile Category Case")
                .contains("Public Profile Tag Case")
                .doesNotContain("public_profile_username_case")
                .doesNotContain("Other Profile Article")
                .doesNotContain("保存简介")
                .doesNotContain("保存昵称")
                .doesNotContain("编辑")
                .doesNotContain("删除")
                .doesNotContain(";jsessionid");
    }

    @Test
    void managementArticleListsDoNotUseWholeRowClickTargets() throws Exception {
        User author = userRepository.save(new User("management_click_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User admin = userRepository.save(new User("management_click_admin_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        Category category = categoryRepository.save(new Category("Management Click Category Case"));
        Article article = articleService.createArticle(form("Management Click Article", "Content", category, List.of()), author.getId());

        String mineHtml = mockMvc.perform(get("/me/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(author.getId(), author.getUsername(), Role.USER)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(mineHtml)
                .contains("Management Click Article")
                .contains("href=\"/articles/" + article.getId() + "\"")
                .doesNotContain("clickable-article")
                .doesNotContain("data-href=")
                .doesNotContain("article-list.js");

        String adminHtml = mockMvc.perform(get("/admin/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(adminHtml)
                .contains("Management Click Article")
                .doesNotContain("clickable-article")
                .doesNotContain("data-href=")
                .doesNotContain("article-list.js");
    }

    @Test
    void userLogoutOnlyAppearsOnHomePageNavigation() throws Exception {
        User user = userRepository.save(new User("logout_home_only_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Logout Home Only Category Case"));
        Article article = articleService.createArticle(form("Logout Home Only Article", "Content", category, List.of()), user.getId());
        SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(), user.getNickname(), Role.USER, null, user.getBio());

        String homeHtml = mockMvc.perform(get("/").sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("退出")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int navIndex = homeHtml.indexOf("<nav class=\"nav\">");
        int userChipIndex = homeHtml.indexOf("class=\"user-chip\"", navIndex);
        int homeLinkIndex = homeHtml.indexOf(">首页</a>", navIndex);
        assertThat(userChipIndex).isGreaterThan(navIndex);
        assertThat(userChipIndex).isLessThan(homeLinkIndex);

        for (String path : List.of(
                "/articles/" + article.getId(),
                "/me/profile",
                "/me/articles",
                "/users/" + user.getId())) {
            mockMvc.perform(get(path).sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/logout\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("class=\"user-chip\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("退出"))));
        }
    }

    @Test
    void adminUserProfilesAreNotPublicAndAdminListLinksOnlyRegularUsers() throws Exception {
        User admin = userRepository.save(new User("admin_public_profile_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        User regularUser = new User("regular_public_profile_case", passwordEncoder.encode("admin123"), Role.USER);
        regularUser.updateNickname("Regular Profile Link");
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(get("/users/" + admin.getId()))
                .andExpect(status().isNotFound());

        String html = mockMvc.perform(get("/admin/users")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html)
                .contains("href=\"/users/" + regularUser.getId() + "\"")
                .contains("Regular Profile Link")
                .doesNotContain("href=\"/users/" + admin.getId() + "\"");

        String profileHtml = mockMvc.perform(get("/users/" + regularUser.getId())
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(profileHtml)
                .contains("href=\"/admin/articles\"")
                .contains("后台")
                .doesNotContain("action=\"/admin/logout\"")
                .doesNotContain("退出");
    }

    @Test
    void anonymousPublicPagesDoNotCreateSessionOrRewriteLinks() throws Exception {
        User author = userRepository.save(new User("anonymous_public_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Anonymous Public Category Case"));
        Article article = articleService.createArticle(form("Anonymous Public Article", "Content", category, List.of()), author.getId());

        var homeResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(homeResult.getRequest().getSession(false)).isNull();
        assertThat(homeResult.getResponse().getContentAsString()).doesNotContain(";jsessionid");

        var detailResult = mockMvc.perform(get("/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(detailResult.getRequest().getSession(false)).isNull();
        assertThat(detailResult.getResponse().getContentAsString()).doesNotContain(";jsessionid");
    }

    @Test
    void userCanUploadAvatarAndSeeItInHeader() throws Exception {
        User user = userRepository.save(new User("upload_avatar_case", passwordEncoder.encode("admin123"), Role.USER));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), Role.USER));
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/me/profile/avatar")
                        .file(avatar)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getAvatarUrl())
                .startsWith("/uploads/avatars/user-" + user.getId() + "-")
                .endsWith(".png");

        SessionUser sessionUser = (SessionUser) session.getAttribute(AuthSession.LOGIN_USER);
        assertThat(sessionUser.avatarUrl()).isEqualTo(savedUser.getAvatarUrl());
        assertThat(sessionUser.bio()).isEqualTo(User.DEFAULT_BIO);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(savedUser.getAvatarUrl())))
                .andExpect(content().string(containsString("upload_avatar_case")));
    }

    @Test
    void invalidAvatarUploadsDoNotReplaceExistingAvatar() throws Exception {
        User user = new User("invalid_avatar_case", passwordEncoder.encode("admin123"), Role.USER);
        user.setAvatarUrl("/uploads/avatars/original.png");
        user = userRepository.save(user);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), Role.USER, user.getAvatarUrl()));

        MockMultipartFile emptyAvatar = new MockMultipartFile("avatar", "empty.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/me/profile/avatar")
                        .file(emptyAvatar)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getAvatarUrl())
                .isEqualTo("/uploads/avatars/original.png");

        MockMultipartFile textAvatar = new MockMultipartFile("avatar", "avatar.txt", "text/plain", "nope".getBytes());
        mockMvc.perform(multipart("/me/profile/avatar")
                        .file(textAvatar)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me/profile"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getAvatarUrl())
                .isEqualTo("/uploads/avatars/original.png");

        MockMultipartFile oversizedAvatar = new MockMultipartFile(
                "avatar",
                "big.png",
                "image/png",
                new byte[2 * 1024 * 1024 + 1]
        );
        Long userId = user.getId();
        assertThatThrownBy(() -> avatarService.updateAvatar(userId, oversizedAvatar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2MB");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getAvatarUrl())
                .isEqualTo("/uploads/avatars/original.png");
    }

    @Test
    void commentsShowUserAvatarsButNotAdminAvatars() throws Exception {
        User articleAuthor = userRepository.save(new User("comment_article_author_case", passwordEncoder.encode("admin123"), Role.USER));
        User avatarUser = new User("comment_avatar_user_case", passwordEncoder.encode("admin123"), Role.USER);
        avatarUser.setAvatarUrl("/uploads/avatars/comment-user.png");
        avatarUser = userRepository.save(avatarUser);
        User defaultAvatarUser = userRepository.save(new User("comment_default_user_case", passwordEncoder.encode("admin123"), Role.USER));
        User admin = new User("comment_admin_avatar_case", passwordEncoder.encode("admin123"), Role.ADMIN);
        admin.setAvatarUrl("/uploads/avatars/admin-hidden.png");
        admin = userRepository.save(admin);
        Category category = categoryRepository.save(new Category("Avatar Comment Category Case"));
        Article article = articleService.createArticle(form("Avatar Comments Article", "Content", category, List.of()), articleAuthor.getId());
        commentRepository.save(new com.tzu.myblogcms.comment.Comment(article, avatarUser, "Avatar comment"));
        commentRepository.save(new com.tzu.myblogcms.comment.Comment(article, defaultAvatarUser, "Default avatar comment"));
        commentRepository.save(new com.tzu.myblogcms.comment.Comment(article, admin, "Admin name only comment"));

        String html = mockMvc.perform(get("/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html)
                .contains("/uploads/avatars/comment-user.png")
                .contains("Avatar comment")
                .contains("Default avatar comment")
                .contains("avatar-placeholder")
                .contains("Admin name only comment")
                .contains("comment_admin_avatar_case")
                .doesNotContain("/uploads/avatars/admin-hidden.png");
    }

    @Test
    void protectedPagesRedirectByRole() throws Exception {
        User user = userRepository.save(new User("protected_user_case", passwordEncoder.encode("admin123"), Role.USER));
        User admin = userRepository.save(new User("protected_admin_case", passwordEncoder.encode("admin123"), Role.ADMIN));

        mockMvc.perform(get("/me/articles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(user.getId(), user.getUsername(), Role.USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/me/articles")
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));
    }

    @Test
    void adminArticleDetailKeepsAdminNavigation() throws Exception {
        User admin = userRepository.save(new User("admin_detail_nav_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        User author = userRepository.save(new User("detail_nav_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Detail Nav Category Case"));
        Article article = articleService.createArticle(form("Detail Navigation Article", "Content", category, List.of()), author.getId());

        String html = mockMvc.perform(get("/articles/" + article.getId())
                        .sessionAttr(AuthSession.LOGIN_USER, new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html)
                .contains("href=\"/admin/articles\"")
                .doesNotContain("href=\"/me/articles\"")
                .doesNotContain("action=\"/articles/" + article.getId() + "/comments\"");
    }

    @Test
    void adminPagesShowLogoutAction() throws Exception {
        User admin = userRepository.save(new User("admin_logout_nav_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        SessionUser sessionUser = new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN);

        for (String path : List.of(
                "/admin/articles",
                "/admin/articles/new",
                "/admin/users",
                "/admin/comments",
                "/admin/categories",
                "/admin/tags")) {
            mockMvc.perform(get(path).sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("action=\"/admin/logout\"")))
                    .andExpect(content().string(containsString("退出")));
        }
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

        String categoriesHtml = mockMvc.perform(get("/admin/categories")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(categoriesHtml)
                .contains("Admin Category Case")
                .doesNotContain("保存");

        String tagsHtml = mockMvc.perform(get("/admin/tags")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(tagsHtml)
                .contains("Admin Tag Case")
                .doesNotContain("保存");

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
    void adminCannotDeleteTaxonomyUsedByArticles() throws Exception {
        User admin = userRepository.save(new User("admin_taxonomy_used_case", passwordEncoder.encode("admin123"), Role.ADMIN));
        SessionUser sessionUser = new SessionUser(admin.getId(), admin.getUsername(), Role.ADMIN);
        User author = userRepository.save(new User("taxonomy_used_author_case", passwordEncoder.encode("admin123"), Role.USER));
        Category category = categoryRepository.save(new Category("Used Category Case"));
        Tag tag = tagRepository.save(new Tag("Used Tag Case"));
        Article article = articleService.createArticle(form("Used Taxonomy Article", "Content", category, List.of(tag)), author.getId());

        String categoryError = "已有文章使用该分类，无法删除";
        mockMvc.perform(post("/admin/categories/" + category.getId() + "/delete")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("error", categoryError));
        assertThat(categoryRepository.findById(category.getId())).isPresent();

        mockMvc.perform(get("/admin/categories")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser)
                        .flashAttr("error", categoryError))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert(")))
                .andExpect(content().string(containsString(categoryError)));

        String tagError = "已有文章使用该标签，无法删除";
        mockMvc.perform(post("/admin/tags/" + tag.getId() + "/delete")
                        .sessionAttr(AuthSession.LOGIN_USER, sessionUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"))
                .andExpect(flash().attribute("error", tagError));
        assertThat(tagRepository.findById(tag.getId())).isPresent();
        assertThat(articleRepository.findById(article.getId())).isPresent();
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
