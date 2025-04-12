package com.huggingsoft.pilot_main.web.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Defines the PasswordEncoder bean that will be used for password hashing.
     * BCrypt is the recommended standard.
     *
     * @return An instance of PasswordEncoder (BCryptPasswordEncoder).
     */
    @Bean // This annotation tells Spring to manage the returned object as a bean
    public PasswordEncoder passwordEncoder() {
        // Use BCryptPasswordEncoder - the industry standard
        return new BCryptPasswordEncoder();
    }

    // You might have other security configurations here (HttpSecurity, etc.)
    // For example:
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
             // ... your security rules ...
             .csrf(AbstractHttpConfigurer::disable); // Example: Disable CSRF if using stateless JWT
         return http.build();
     }
}
