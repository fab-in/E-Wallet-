package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private ServerWebExchange exchange;
    private MockServerHttpRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationFilter, "excludedPathsString", 
                "/auth/signup,/auth/login,/actuator");
        authenticationFilter.init();
    }

    @Test
    void testFilter_ExcludedPath_ShouldPassThrough() {
        request = MockServerHttpRequest.get("/auth/login").build();
        exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_ExcludedPathSignup_ShouldPassThrough() {
        request = MockServerHttpRequest.post("/auth/signup").build();
        exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_ExcludedPathActuator_ShouldPassThrough() {
        request = MockServerHttpRequest.get("/actuator/health").build();
        exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_MissingAuthorizationHeader_ShouldReturnUnauthorized() {
        request = MockServerHttpRequest.get("/users/123").build();
        exchange = MockServerWebExchange.from(request);

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, never()).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_InvalidAuthorizationHeader_ShouldReturnUnauthorized() {
        request = MockServerHttpRequest.get("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "InvalidHeader")
                .build();
        exchange = MockServerWebExchange.from(request);

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, never()).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_InvalidToken_ShouldReturnUnauthorized() {
        String token = "invalid.token";
        request = MockServerHttpRequest.get("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(false);

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(token);
        verify(filterChain, never()).filter(any(ServerWebExchange.class));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ValidToken_ShouldAddHeadersAndContinue() {
        String token = "valid.token";
        String email = "test@example.com";
        String userId = "user123";
        String role = "USER";

        request = MockServerHttpRequest.get("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(token)).thenReturn(email);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getRoleFromToken(token)).thenReturn(role);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(token);
        verify(jwtUtil, times(1)).getEmailFromToken(token);
        verify(jwtUtil, times(1)).getUserIdFromToken(token);
        verify(jwtUtil, times(1)).getRoleFromToken(token);
        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));

        ServerHttpRequest modifiedRequest = exchange.getRequest();
        assertEquals(email, modifiedRequest.getHeaders().getFirst("X-User-Email"));
        assertEquals(userId, modifiedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals(role, modifiedRequest.getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void testFilter_ValidTokenWithNullRole_ShouldUseDefaultRole() {
        String token = "valid.token";
        String email = "test@example.com";
        String userId = "user123";

        request = MockServerHttpRequest.get("/wallets/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(token)).thenReturn(email);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getRoleFromToken(token)).thenReturn(null);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(token);
        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));

        ServerHttpRequest modifiedRequest = exchange.getRequest();
        assertEquals("USER", modifiedRequest.getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void testFilter_ValidTokenWithAdminRole_ShouldSetAdminRole() {
        String token = "valid.token";
        String email = "admin@example.com";
        String userId = "admin123";
        String role = "ADMIN";

        request = MockServerHttpRequest.get("/transactions/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(token)).thenReturn(email);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getRoleFromToken(token)).thenReturn(role);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        ServerHttpRequest modifiedRequest = exchange.getRequest();
        assertEquals(role, modifiedRequest.getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void testFilter_ExceptionWhenExtractingClaims_ShouldReturnUnauthorized() {
        String token = "valid.token";

        request = MockServerHttpRequest.get("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(token);
        verify(filterChain, never()).filter(any(ServerWebExchange.class));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testIsExcludedPath_ExcludedPath_ReturnsTrue() {
        request = MockServerHttpRequest.get("/auth/login").build();
        exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testIsExcludedPath_NonExcludedPath_ReturnsFalse() {
        request = MockServerHttpRequest.get("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build();
        exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(anyString())).thenReturn(false);

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, times(1)).validateToken(anyString());
    }

    @Test
    void testGetOrder_ReturnsCorrectOrder() {
        int order = authenticationFilter.getOrder();
        assertEquals(-1, order);
    }

    @Test
    void testFilter_PathStartingWithExcluded_ShouldPassThrough() {
        request = MockServerHttpRequest.get("/auth/signup/verify").build();
        exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verify(jwtUtil, never()).validateToken(anyString());
    }
}
