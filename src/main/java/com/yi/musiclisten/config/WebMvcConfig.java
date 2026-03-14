package com.yi.musiclisten.config;

import com.yi.musiclisten.filter.JwtAuthenticationFilter;
import com.yi.musiclisten.intercepter.JwtIntercepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtIntercepter  jwtIntercepter;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:8090") // 前端地址
                        .allowedMethods("*")
                        .allowCredentials(true);
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtIntercepter)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/sendEmailCode",
                        "/register",
                        "/song/**",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/playlist/recommend",
                        "/playlist/*",
                        "/album/recommend",
                        "/album/detail/*",
                        "/follow/tabCount",
                        "/user/search",
                        "/user/*/public")
                .order(Ordered.HIGHEST_PRECEDENCE); // 设置为最高优先级
    }
}