package com.finance.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // This grabs the Vercel URL you just put into Render!
    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all API routes
                .allowedOrigins(frontendUrl) // The VIP Vercel URL
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // OPTIONS is crucial for fixing the preflight error!
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
