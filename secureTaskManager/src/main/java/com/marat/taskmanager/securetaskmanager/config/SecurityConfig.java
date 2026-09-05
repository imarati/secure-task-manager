package com.marat.taskmanager.securetaskmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marat.taskmanager.securetaskmanager.exception.ApiError;
import com.marat.taskmanager.securetaskmanager.security.JwtAuthenticationFilter;
import com.marat.taskmanager.securetaskmanager.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<RateLimitFilter> rateLimitFilterProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            ApiError error = ApiError.builder()
                                    .timestamp(java.time.Instant.now())
                                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                                    .error("Unauthorized")
                                    .message("Authentication is required")
                                    .path(request.getRequestURI())
                                    .build();

                            response.setStatus(
                                    HttpServletResponse.SC_UNAUTHORIZED
                            );
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    error
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ApiError error = ApiError.builder()
                                    .timestamp(java.time.Instant.now())
                                    .status(HttpServletResponse.SC_FORBIDDEN)
                                    .error("Forbidden")
                                    .message(
                                            "You do not have permission "
                                                    + "to perform this action"
                                    )
                                    .path(request.getRequestURI())
                                    .build();

                            response.setStatus(
                                    HttpServletResponse.SC_FORBIDDEN
                            );
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    error
                            );
                        })
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        RateLimitFilter rateLimitFilter =
                rateLimitFilterProvider.getIfAvailable();

        if (rateLimitFilter != null) {
            http.addFilterBefore(
                    rateLimitFilter,
                    UsernamePasswordAuthenticationFilter.class
            );
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}