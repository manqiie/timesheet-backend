// Updated SecurityConfig.java with role-based access control
package com.goldtech.timesheet_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable method-level security
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtRequestFilter jwtRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Explicitly set BCrypt strength to 10 to match database hashes
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers("/auth/login", "/auth/validate").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/test/**").permitAll()  // For testing

                        // User management endpoints - role-based access
                        .requestMatchers("/users").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/users/managers").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/users/roles").hasRole("ADMIN")
                        .requestMatchers("/users/stats").hasRole("ADMIN")
                        .requestMatchers("/users/*/status").hasRole("ADMIN")
                        .requestMatchers("/users/*/reset-password").hasRole("ADMIN")
                        .requestMatchers("/users/bulk").hasRole("ADMIN")

                        // GET single user - admins and managers
                        .requestMatchers("GET", "/users/*").hasAnyRole("ADMIN", "MANAGER")

                        // POST (create) - admin only
                        .requestMatchers("POST", "/users").hasRole("ADMIN")

                        // PUT (update) - admin only
                        .requestMatchers("PUT", "/users/*").hasRole("ADMIN")

                        // DELETE - admin only
                        .requestMatchers("DELETE", "/users/*").hasRole("ADMIN")

                        // User profile endpoints
                        .requestMatchers("/auth/me").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                );

        // Add JWT filter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}