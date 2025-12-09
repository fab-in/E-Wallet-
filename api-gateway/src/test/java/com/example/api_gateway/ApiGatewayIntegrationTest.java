package com.example.api_gateway;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "gateway.auth.excluded-paths=/auth/signup,/auth/login,/actuator",
        "jwt.secret=MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm",
        "management.endpoints.web.exposure.include=health,metrics,gateway",
        "management.endpoint.health.enabled=true",
        "management.endpoints.web.base-path=/actuator",
        "spring.cloud.gateway.routes[0].id=user-service",
        "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/users/**,/auth/**",
        "spring.cloud.gateway.routes[1].id=wallet-service",
        "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
        "spring.cloud.gateway.routes[1].predicates[0]=Path=/wallets/**",
        "spring.cloud.gateway.routes[2].id=transaction-service",
        "spring.cloud.gateway.routes[2].uri=http://localhost:8083",
        "spring.cloud.gateway.routes[2].predicates[0]=Path=/transactions/**"
})
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;
    private String expiredToken;
    private String email = "test@example.com";
    private String userId = "user123";
    private String role = "USER";

    @BeforeEach
    void setUp() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000); // 1 hour
        
        SecretKey testSecretKey = Keys.hmacShaKeyFor(
                "MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm".getBytes()
        );
        
        validToken = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(testSecretKey)
                .compact();

        Date pastDate = new Date(now.getTime() - 3600000);
        expiredToken = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(pastDate)
                .expiration(pastDate)
                .signWith(testSecretKey)
                .compact();
    }

    @Test
    void testExcludedPath_Actuator_ShouldPassThrough() {
        org.springframework.http.HttpStatusCode statusCode = webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectBody()
                .returnResult()
                .getStatus();
        
        assertNotEquals(
            org.springframework.http.HttpStatus.UNAUTHORIZED.value(),
            statusCode.value(),
            "Filter should allow actuator endpoints through without authentication"
        );
    }

    @Test
    void testProtectedPath_WithoutToken_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/users/123")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").exists();
    }

    @Test
    void testProtectedPath_WithInvalidToken_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").exists();
    }

    @Test
    void testProtectedPath_WithExpiredToken_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/users/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json");
    }

    @Test
    void testProtectedPath_WithInvalidHeaderFormat_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/wallets/123")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json");
    }

    @Test
    void testProtectedPath_MissingAuthorizationHeader_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/transactions/123")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }
}
