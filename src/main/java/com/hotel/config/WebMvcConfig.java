package com.hotel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Spring Web MVC configuration for static asset caching, performance optimization,
 * and seamless reverse proxy / public tunnel CORS support.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FlexibleLocalDateConverter flexibleLocalDateConverter;

    public WebMvcConfig(FlexibleLocalDateConverter flexibleLocalDateConverter) {
        this.flexibleLocalDateConverter = flexibleLocalDateConverter;
    }

    @Override
    public void addFormatters(org.springframework.format.FormatterRegistry registry) {
        registry.addConverter(flexibleLocalDateConverter);
        registry.addFormatter(flexibleLocalDateConverter);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // High-performance browser and CDN/tunnel caching for static images & animation frames
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

        // Cache CSS stylesheets for 7 days
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());

        // Cache JavaScript files for 7 days
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
