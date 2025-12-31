package org.springframework.samples.petclinic.api.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SecurityHeadersConfig.
 * Tests that all security headers are properly added to HTTP responses.
 */
class SecurityHeadersConfigTest {

    private SecurityHeadersConfig securityHeadersConfig;
    private WebFilter securityHeadersFilter;

    @BeforeEach
    void setUp() {
        securityHeadersConfig = new SecurityHeadersConfig();
        securityHeadersFilter = securityHeadersConfig.securityHeadersFilter();
    }

    @Test
    void securityHeadersFilter_shouldReturnNonNullBean() {
        assertThat(securityHeadersFilter).isNotNull();
    }

    @Test
    void securityHeadersFilter_shouldAddContentSecurityPolicyHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("Content-Security-Policy"))
            .isNotNull()
            .contains("default-src 'self'")
            .contains("script-src 'self' 'unsafe-inline' 'unsafe-eval'")
            .contains("style-src 'self' 'unsafe-inline'")
            .contains("img-src 'self' data:")
            .contains("font-src 'self' data:")
            .contains("frame-ancestors 'self'")
            .contains("form-action 'self'");
    }

    @Test
    void securityHeadersFilter_shouldAddXContentTypeOptionsHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void securityHeadersFilter_shouldAddXXssProtectionHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
    }

    @Test
    void securityHeadersFilter_shouldAddXFrameOptionsHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/owners").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }

    @Test
    void securityHeadersFilter_shouldAddReferrerPolicyHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/pets").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    void securityHeadersFilter_shouldAddPermissionsPolicyHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/vets").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("Permissions-Policy"))
            .isEqualTo("geolocation=(), microphone=(), camera=()");
    }

    @Test
    void securityHeadersFilter_shouldAddAllSecurityHeaders() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/owners").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
        
        HttpHeaders headers = exchange.getResponse().getHeaders();
        
        // Verify all 6 security headers are present
        assertThat(headers.getFirst("Content-Security-Policy")).isNotNull();
        assertThat(headers.getFirst("X-Content-Type-Options")).isNotNull();
        assertThat(headers.getFirst("X-XSS-Protection")).isNotNull();
        assertThat(headers.getFirst("X-Frame-Options")).isNotNull();
        assertThat(headers.getFirst("Referrer-Policy")).isNotNull();
        assertThat(headers.getFirst("Permissions-Policy")).isNotNull();
    }

    @Test
    void securityHeadersFilter_shouldWorkWithDifferentHttpMethods() {
        // Test with PUT request
        MockServerHttpRequest putRequest = MockServerHttpRequest.put("/api/owners/1").build();
        MockServerWebExchange putExchange = MockServerWebExchange.from(putRequest);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> putResult = securityHeadersFilter.filter(putExchange, chain);
        StepVerifier.create(putResult).verifyComplete();
        
        assertThat(putExchange.getResponse().getHeaders().getFirst("X-Frame-Options"))
            .isEqualTo("SAMEORIGIN");

        // Test with DELETE request
        MockServerHttpRequest deleteRequest = MockServerHttpRequest.delete("/api/pets/1").build();
        MockServerWebExchange deleteExchange = MockServerWebExchange.from(deleteRequest);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> deleteResult = securityHeadersFilter.filter(deleteExchange, chain);
        StepVerifier.create(deleteResult).verifyComplete();
        
        assertThat(deleteExchange.getResponse().getHeaders().getFirst("Content-Security-Policy"))
            .isNotNull();
    }

    @Test
    void securityHeadersFilter_shouldCallFilterChain() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = securityHeadersFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
    }
}
