package com.envoil.admin.config;

import com.envoil.admin.auth.AdminJwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AdminJwtAuthInterceptor jwtAuthInterceptor;

    public AdminWebMvcConfig(AdminJwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/admin/**");
    }
}
