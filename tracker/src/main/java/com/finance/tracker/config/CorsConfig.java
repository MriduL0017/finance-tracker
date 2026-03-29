package com.finance.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
	
	@Value("${frontend.url}")
	private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply this rule to ALL endpoints (/api/expenses, /api/users, etc.)
                .allowedOrigins(frontendUrl) // Allow only the React frontend to access these APIs
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow these HTTP actions
                .allowedHeaders("*"); // Allow any headers (like JSON content types)
    }
}