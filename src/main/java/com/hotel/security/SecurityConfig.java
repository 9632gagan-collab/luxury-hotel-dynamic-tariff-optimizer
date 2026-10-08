package com.hotel.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration for Stage 2.
 * Enforces role-based access control across Guest, Staff, and Admin portals.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationProvider authenticationProvider;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Autowired
    public SecurityConfig(CustomAuthenticationProvider authenticationProvider,
                          CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.authenticationProvider = authenticationProvider;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/css/**", "/js/**", "/images/**", "/favicon.ico", "/manifest.json", "/sw.js")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.disable())
            .securityContext(sc -> sc.disable())
            .sessionManagement(sm -> sm.disable())
            .requestCache(rc -> rc.disable());
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/login", "/logout"))
            .authenticationProvider(authenticationProvider)
            .headers(headers -> headers
                .cacheControl(org.springframework.security.config.Customizer.withDefaults())
                .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                .frameOptions(frame -> frame.sameOrigin())
            )
            .sessionManagement(session -> session
                .sessionFixation(sf -> sf.changeSessionId())
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                // Publicly accessible endpoints
                .requestMatchers(
                    "/",
                    "/health",
                    "/actuator/health",
                    "/manifest.json",
                    "/sw.js",
                    "/login",
                    "/register",
                    "/demo",
                    "/demo/**",
                    "/access-denied",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    "/rooms/**",
                    "/hotels/**",
                    "/map",
                    "/map/**",
                    "/api/**",
                    "/districts",
                    "/offers",
                    "/guest/districts",
                    "/guest/offers"
                ).permitAll()
                // Role-based protected portals
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasRole("STAFF")
                .requestMatchers("/guest/**").hasRole("GUEST")
                .requestMatchers("/bookings/**").hasRole("GUEST")
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .requestCache(cache -> cache
                .requestCache(new org.springframework.security.web.savedrequest.HttpSessionRequestCache())
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
