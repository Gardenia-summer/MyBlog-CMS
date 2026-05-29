package com.tzu.myblogcms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminLoginInterceptor adminLoginInterceptor;
    private final UserLoginInterceptor userLoginInterceptor;
    private final String avatarDir;

    public WebConfig(AdminLoginInterceptor adminLoginInterceptor,
                     UserLoginInterceptor userLoginInterceptor,
                     @Value("${app.upload.avatar-dir:uploads/avatars}") String avatarDir) {
        this.adminLoginInterceptor = adminLoginInterceptor;
        this.userLoginInterceptor = userLoginInterceptor;
        this.avatarDir = avatarDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
        registry.addInterceptor(userLoginInterceptor)
                .addPathPatterns("/me/**", "/articles/*/comments", "/articles/*/like");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatarResourceLocation());
    }

    private String avatarResourceLocation() {
        String location = Path.of(avatarDir).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
