package com.example.api_gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private SecretKey testSecretKey;
    private String validToken;
    private String expiredToken;
    private String tokenWithClaims;

    @BeforeEach
    void setUp() {
        String secretKeyString = "MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm";
        byte[] keyBytes = secretKeyString.getBytes();
        byte[] finalKeyBytes = new byte[32];
        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, finalKeyBytes, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, finalKeyBytes, 0, keyBytes.length);
            for (int i = keyBytes.length; i < 32; i++) {
                finalKeyBytes[i] = keyBytes[i % keyBytes.length];
            }
        }
        testSecretKey = Keys.hmacShaKeyFor(finalKeyBytes);

        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", secretKeyString);
        ReflectionTestUtils.setField(jwtUtil, "secretKey", testSecretKey);

        jwtUtil.initializeSecretKey();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000); // 1 hour
        validToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", "user123")
                .claim("role", "USER")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(testSecretKey)
                .compact();

        Date pastDate = new Date(now.getTime() - 3600000); // 1 hour ago
        expiredToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", "user123")
                .claim("role", "USER")
                .issuedAt(pastDate)
                .expiration(pastDate)
                .signWith(testSecretKey)
                .compact();

        tokenWithClaims = Jwts.builder()
                .subject("user@example.com")
                .claim("userId", "user456")
                .claim("role", "ADMIN")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(testSecretKey)
                .compact();
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        assertTrue(jwtUtil.validateToken(validToken));
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testValidateToken_NullToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void testValidateToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        String malformedToken = "not.a.valid.jwt.token";
        assertFalse(jwtUtil.validateToken(malformedToken));
    }

    @Test
    void testValidateToken_TokenWithWrongSignature_ReturnsFalse() {
        SecretKey differentKey = Keys.hmacShaKeyFor("DifferentSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong".getBytes());
        String wrongSignatureToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        assertFalse(jwtUtil.validateToken(wrongSignatureToken));
    }

    @Test
    void testGetEmailFromToken_ValidToken_ReturnsEmail() {
        String email = jwtUtil.getEmailFromToken(validToken);
        assertEquals("test@example.com", email);
    }

    @Test
    void testGetEmailFromToken_InvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getEmailFromToken("invalid.token");
        });
    }

    @Test
    void testGetUserIdFromToken_ValidToken_ReturnsUserId() {
        String userId = jwtUtil.getUserIdFromToken(validToken);
        assertEquals("user123", userId);
    }

    @Test
    void testGetUserIdFromToken_TokenWithClaims_ReturnsCorrectUserId() {
        String userId = jwtUtil.getUserIdFromToken(tokenWithClaims);
        assertEquals("user456", userId);
    }

    @Test
    void testGetUserIdFromToken_InvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken("invalid.token");
        });
    }

    @Test
    void testGetRoleFromToken_ValidToken_ReturnsRole() {
        String role = jwtUtil.getRoleFromToken(validToken);
        assertEquals("USER", role);
    }

    @Test
    void testGetRoleFromToken_TokenWithAdminRole_ReturnsAdmin() {
        String role = jwtUtil.getRoleFromToken(tokenWithClaims);
        assertEquals("ADMIN", role);
    }

    @Test
    void testGetRoleFromToken_TokenWithoutRole_ReturnsNull() {
        Date now = new Date();
        String tokenWithoutRole = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", "user123")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000))
                .signWith(testSecretKey)
                .compact();

        String role = jwtUtil.getRoleFromToken(tokenWithoutRole);
        assertNull(role);
    }

    @Test
    void testGetRoleFromToken_InvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getRoleFromToken("invalid.token");
        });
    }

    @Test
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        assertFalse(jwtUtil.isTokenExpired(validToken));
    }

    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        assertTrue(jwtUtil.isTokenExpired(expiredToken));
    }

    @Test
    void testIsTokenExpired_InvalidToken_ReturnsTrue() {
        assertTrue(jwtUtil.isTokenExpired("invalid.token"));
    }

    @Test
    void testIsTokenExpired_NullToken_ReturnsTrue() {
        assertTrue(jwtUtil.isTokenExpired(null));
    }

    @Test
    void testInitializeSecretKey_ValidSecretKey_InitializesSuccessfully() {
        JwtUtil newJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(newJwtUtil, "secretKeyString", 
                "MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm");
        
        assertDoesNotThrow(() -> newJwtUtil.initializeSecretKey());
        
        SecretKey secretKey = (SecretKey) ReflectionTestUtils.getField(newJwtUtil, "secretKey");
        assertNotNull(secretKey);
    }

    @Test
    void testInitializeSecretKey_ShortSecretKey_PadsCorrectly() {
        JwtUtil newJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(newJwtUtil, "secretKeyString", "short");
        
        assertDoesNotThrow(() -> newJwtUtil.initializeSecretKey());
        
        SecretKey secretKey = (SecretKey) ReflectionTestUtils.getField(newJwtUtil, "secretKey");
        assertNotNull(secretKey);
    }

    @Test
    void testGetEmailFromToken_TokenWithClaims_ReturnsCorrectEmail() {
        String email = jwtUtil.getEmailFromToken(tokenWithClaims);
        assertEquals("user@example.com", email);
    }
}
