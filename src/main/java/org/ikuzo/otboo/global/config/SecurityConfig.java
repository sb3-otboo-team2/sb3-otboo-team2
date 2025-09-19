package org.ikuzo.otboo.global.config;

import org.ikuzo.otboo.global.security.JwtAuthenticationFilter;
import org.ikuzo.otboo.global.security.JwtLoginSuccessHandler;
import org.ikuzo.otboo.global.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtLoginSuccessHandler jwtLoginSuccessHandler,
        LoginFailureHandler loginFailureHandler,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/sign-in")
                .successHandler(jwtLoginSuccessHandler)
                .failureHandler(loginFailureHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(request ->
                        !request.getRequestURI().startsWith("/api/")
                ).permitAll()
                .anyRequest().authenticated()
//                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

