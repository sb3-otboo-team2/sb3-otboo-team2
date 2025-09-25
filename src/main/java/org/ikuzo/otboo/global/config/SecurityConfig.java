package org.ikuzo.otboo.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikuzo.otboo.domain.user.entity.Role;
import org.ikuzo.otboo.global.oauth2.handler.Oauth2LoginSuccessHandler;
import org.ikuzo.otboo.global.security.Http403ForbiddenAccessDeniedHandler;
import org.ikuzo.otboo.global.security.JwtAuthenticationFilter;
import org.ikuzo.otboo.global.security.JwtLoginSuccessHandler;
import org.ikuzo.otboo.global.security.JwtLogoutHandler;
import org.ikuzo.otboo.global.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtLoginSuccessHandler jwtLoginSuccessHandler,
        LoginFailureHandler loginFailureHandler,
        ObjectMapper objectMapper,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtLogoutHandler jwtLogoutHandler,
        Oauth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/sign-in")
                .successHandler(jwtLoginSuccessHandler)
                .failureHandler(loginFailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/sign-out")
                .addLogoutHandler(jwtLogoutHandler)
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization"))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*"))
                .successHandler(oauth2LoginSuccessHandler)
            )
            .authorizeHttpRequests(auth -> auth
//                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
//                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
//                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
//                .requestMatchers(request ->
//                        !request.getRequestURI().startsWith("/api/")
//                ).permitAll()
//                .anyRequest().authenticated()
                    .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .accessDeniedHandler(new Http403ForbiddenAccessDeniedHandler(objectMapper))
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

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
            .role(Role.ADMIN.name())
            .implies(Role.USER.name())
            .build();
    }
}

