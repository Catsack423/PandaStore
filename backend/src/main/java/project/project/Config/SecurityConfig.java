package project.project.Config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import project.project.Filter.JwtAuthenticationFilter;
import project.project.Filter.LoginRateLimitFilter;
import project.project.Filter.SellerApplicationAdminFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt,
            LoginRateLimitFilter rateLimit, SellerApplicationAdminFilter sellerAdmin) throws Exception {
        return http
                .cors(cors -> {})
                // This API accepts explicit Bearer headers, never browser authentication cookies.
                // Cookie-authenticated Next.js routes separately enforce same-origin writes.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Please sign in\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register/customer",
                                "/api/auth/register/seller", "/api/sellers/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*",
                                "/api/products/seller/*", "/api/categories", "/api/categories/*/products",
                                "/api/reviews/product/*", "/api/seller/shops/*", "/api/seller/shops/user/*").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/products", "/api/products/*",
                                "/api/categories", "/api/reviews/product/*").permitAll()
                        .requestMatchers("/api/test/**", "/api/admin/**", "/api/customers").hasRole("ADMIN")
                        // This demo lets customers simulate payment for their own order only.
                        .requestMatchers(HttpMethod.POST, "/api/payments/simulate").hasAnyRole("CUSTOMER", "ADMIN")
                        // Callbacks and refunds remain administrative.
                        .requestMatchers("/api/payments/callback", "/api/payments/simulate",
                                "/api/payments/refund/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, AnonymousAuthenticationFilter.class)
                .addFilterBefore(rateLimit, JwtAuthenticationFilter.class)
                .addFilterAfter(sellerAdmin, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.allowed-origins:http://localhost:3000}") String origins) {
        List<String> allowed = Arrays.stream(origins.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (allowed.isEmpty() || allowed.stream().anyMatch(value -> value.contains("*")
                || !value.matches("https?://[^/]+"))) {
            throw new IllegalArgumentException("CORS requires explicit http(s) origins without wildcards or paths");
        }
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowed);
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Each filter runs once, inside Spring Security, after security headers and CORS.
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<LoginRateLimitFilter> rateLimitRegistration(LoginRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<SellerApplicationAdminFilter> sellerAdminRegistration(SellerApplicationAdminFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
