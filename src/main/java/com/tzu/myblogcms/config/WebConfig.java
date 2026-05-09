package com.tzu.myblogcms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminLoginInterceptor adminLoginInterceptor;
    private final UserLoginInterceptor userLoginInterceptor;

    public WebConfig(AdminLoginInterceptor adminLoginInterceptor, UserLoginInterceptor userLoginInterceptor) {
        this.adminLoginInterceptor = adminLoginInterceptor;
        this.userLoginInterceptor = userLoginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
        registry.addInterceptor(userLoginInterceptor)
                .addPathPatterns("/me/**", "/articles/*/comments");
    }
}
