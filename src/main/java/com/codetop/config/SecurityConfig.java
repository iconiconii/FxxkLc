package com.codetop.config;

import com.codetop.security.JwtAuthenticationEntryPoint;
import com.codetop.security.JwtAuthenticationFilter;
import com.codetop.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Security configuration for JWT-based authentication.
 * 
 * @author CodeTop Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.security.druid.enabled:false}")
    private boolean druidSecurityEnabled;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final @Lazy JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    @Value("${app.docs.enabled:false}")
    private boolean docsEnabled;
    @Value("${app.security.ai-recs-public:false}")
    private boolean aiRecsPublic;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - ONLY the three required by user
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/codetop/problems/global").permitAll()
                        .requestMatchers(HttpMethod.GET, "/filter/companies").permitAll()
                        // AI recommendations endpoints - public only when explicitly allowed (dev)
                        .requestMatchers(HttpMethod.GET, "/problems/ai-recommendations")
                            .access((auth, ctx) -> aiRecsPublic
                                    ? new AuthorizationDecision(true)
                                    : new AuthorizationDecision(auth.get() != null && auth.get().isAuthenticated()))
                        .requestMatchers(HttpMethod.POST, "/problems/*/recommendation-feedback")
                            .access((auth, ctx) -> aiRecsPublic
                                    ? new AuthorizationDecision(true)
                                    : new AuthorizationDecision(auth.get() != null && auth.get().isAuthenticated()))
                        // API docs - dev enabled or restrict to ADMIN
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                            .access((auth, ctx) -> {
                                if (docsEnabled) {
                                    return org.springframework.security.authorization.AuthorityAuthorizationManager.hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                                            .check(auth, ctx.getRequest());
                                } else {
                                    return org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority("ROLE_ADMIN")
                                            .check(auth, ctx.getRequest());
                                }
                            })
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/codetop/health").permitAll()

                        // Admin endpoints
                        .requestMatchers("/druid/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/problems/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/problems/**").hasRole("ADMIN")

                        // ALL other endpoints require authentication (per user requirements)
                        .anyRequest().authenticated());

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Add rate limit filter after JWT filter
        // This ensures authentication happens before rate limiting
        http.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Environment-specific allowed origins (no wildcards in production)
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(allowedOrigins);
        } else {
            // Fallback for development only
            configuration.setAllowedOriginPatterns(Collections.singletonList("http://localhost:*"));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        // Security headers
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "Link"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // PasswordEncoder moved to PasswordEncoderConfig to break circular dependency

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
