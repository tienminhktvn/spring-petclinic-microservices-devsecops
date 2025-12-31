package org.springframework.samples.petclinic.api.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.WebFilter;

/**
 * Security configuration to mitigate vulnerabilities in legacy AngularJS library.
 * Adds Content Security Policy and other security headers.
 */
@Configuration
public class SecurityHeadersConfig {

    @Bean
    public WebFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            // Content Security Policy to mitigate XSS attacks
            // Note: 'unsafe-eval' and 'unsafe-inline' are required for AngularJS to work
            headers.add("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self' data:; " +
                "frame-ancestors 'self'; " +
                "form-action 'self'");
            
            // Prevent MIME type sniffing
            headers.add("X-Content-Type-Options", "nosniff");
            
            // Enable XSS filter in browsers
            headers.add("X-XSS-Protection", "1; mode=block");
            
            // Prevent clickjacking
            headers.add("X-Frame-Options", "SAMEORIGIN");
            
            // Control referrer information
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Permissions policy (disable sensitive features)
            headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            
            return chain.filter(exchange);
        };
    }
}
