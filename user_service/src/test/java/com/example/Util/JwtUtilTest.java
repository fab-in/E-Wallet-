package com.example.Util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private UUID testUserId;
    private String testEmail;
    private String testRole;
    private String secretKeyString;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testRole = "USER";
        secretKeyString = "MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm";

        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", secretKeyString);
        jwtUtil.initializeSecretKey();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Should generate token with default role when role is null")
    void testGenerateToken_DefaultRole() {
        String token = jwtUtil.generateToken(testUserId, testEmail, null);

        assertNotNull(token);
        String role = jwtUtil.getRoleFromToken(token);
        assertEquals("USER", role);
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken_ValidToken() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for null token")
    void testValidateToken_NullToken() {
        boolean isValid = jwtUtil.validateToken(null);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for empty token")
    void testValidateToken_EmptyToken() {
        boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for expired token")
    void testValidateToken_ExpiredToken() throws InterruptedException {
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtUtil, "secretKey");
        Date pastDate = new Date(System.currentTimeMillis() - 100000);

        String expiredToken = Jwts.builder()
                .subject(testEmail)
                .claim("userId", testUserId.toString())
                .claim("role", testRole)
                .issuedAt(pastDate)
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        boolean isValid = jwtUtil.validateToken(expiredToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract email from token")
    void testGetEmailFromToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        String email = jwtUtil.getEmailFromToken(token);

        assertEquals(testEmail, email);
    }

    @Test
    @DisplayName("Should extract userId from token")
    void testGetUserIdFromToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        UUID userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(testUserId, userId);
    }

    @Test
    @DisplayName("Should extract role from token")
    void testGetRoleFromToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        String role = jwtUtil.getRoleFromToken(token);

        assertEquals(testRole, role);
    }

    @Test
    @DisplayName("Should extract role ADMIN from token")
    void testGetRoleFromToken_Admin() {
        String token = jwtUtil.generateToken(testUserId, testEmail, "ADMIN");

        String role = jwtUtil.getRoleFromToken(token);

        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("Should return expiration date from token")
    void testGetExpirationDateFromToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    @DisplayName("Should return null expiration date for invalid token")
    void testGetExpirationDateFromToken_InvalidToken() {
        Date expirationDate = jwtUtil.getExpirationDateFromToken("invalid.token");

        assertNull(expirationDate);
    }

    @Test
    @DisplayName("Should return issued at date from token")
    void testGetIssuedAtDateFromToken_Success() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        Date issuedAt = jwtUtil.getIssuedAtDateFromToken(token);

        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }

    @Test
    @DisplayName("Should return null issued at date for invalid token")
    void testGetIssuedAtDateFromToken_InvalidToken() {
        Date issuedAt = jwtUtil.getIssuedAtDateFromToken("invalid.token");

        assertNull(issuedAt);
    }

    @Test
    @DisplayName("Should return false for non-expired token")
    void testIsTokenExpired_NotExpired() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should return true for expired token")
    void testIsTokenExpired_Expired() throws InterruptedException {
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtUtil, "secretKey");

        String expiredToken = Jwts.builder()
                .subject(testEmail)
                .claim("userId", testUserId.toString())
                .claim("role", testRole)
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        boolean isExpired = jwtUtil.isTokenExpired(expiredToken);

        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true for invalid token in isTokenExpired")
    void testIsTokenExpired_InvalidToken() {
        boolean isExpired = jwtUtil.isTokenExpired("invalid.token");

        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return expiration time constant")
    void testGetExpirationTime() {
        long expirationTime = jwtUtil.getExpirationTime();

        assertEquals(30 * 60 * 1000, expirationTime);
    }

    @Test
    @DisplayName("Should generate different tokens for same user")
    void testGenerateToken_Uniqueness() {
        String token1 = jwtUtil.generateToken(testUserId, testEmail, testRole);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token2 = jwtUtil.generateToken(testUserId, testEmail, testRole);

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should extract correct claims from token")
    void testTokenClaims_AllFields() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);

        String email = jwtUtil.getEmailFromToken(token);
        UUID userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        assertEquals(testEmail, email);
        assertEquals(testUserId, userId);
        assertEquals(testRole, role);
    }

    @Test
    @DisplayName("Should initialize secret key with short key (padding)")
    void testInitializeSecretKey_ShortKey() {
        JwtUtil shortKeyUtil = new JwtUtil();
        String shortKey = "ShortKey";
        ReflectionTestUtils.setField(shortKeyUtil, "secretKeyString", shortKey);
        
        assertDoesNotThrow(() -> shortKeyUtil.initializeSecretKey());
        assertNotNull(ReflectionTestUtils.getField(shortKeyUtil, "secretKey"));
    }

    @Test
    @DisplayName("Should initialize secret key with exact 32 byte key")
    void testInitializeSecretKey_Exact32Bytes() {
        JwtUtil exactKeyUtil = new JwtUtil();
        String exactKey = "12345678901234567890123456789012"; 
        ReflectionTestUtils.setField(exactKeyUtil, "secretKeyString", exactKey);
        
        assertDoesNotThrow(() -> exactKeyUtil.initializeSecretKey());
        assertNotNull(ReflectionTestUtils.getField(exactKeyUtil, "secretKey"));
    }

    @Test
    @DisplayName("Should handle exception in initializeSecretKey and throw RuntimeException")
    void testInitializeSecretKey_ExceptionHandling() {
        JwtUtil badKeyUtil = new JwtUtil();
        String testKey = "TestKey";
        ReflectionTestUtils.setField(badKeyUtil, "secretKeyString", testKey);
        
        try (MockedStatic<Keys> keysMock = mockStatic(Keys.class)) {
            keysMock.when(() -> Keys.hmacShaKeyFor(any(byte[].class)))
                    .thenThrow(new RuntimeException("Key generation failed"));
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                badKeyUtil.initializeSecretKey();
            });
            
            assertEquals("Failed to initialize JWT secret key", exception.getMessage());
            assertNotNull(exception.getCause());
            assertEquals("Key generation failed", exception.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Should handle exception in getEmailFromToken")
    void testGetEmailFromToken_Exception() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> {
            jwtUtil.getEmailFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should handle exception in getUserIdFromToken")
    void testGetUserIdFromToken_Exception() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should handle exception in getRoleFromToken")
    void testGetRoleFromToken_Exception() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> {
            jwtUtil.getRoleFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should handle null token in getEmailFromToken")
    void testGetEmailFromToken_NullToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getEmailFromToken(null);
        });
    }

    @Test
    @DisplayName("Should handle null token in getUserIdFromToken")
    void testGetUserIdFromToken_NullToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(null);
        });
    }

    @Test
    @DisplayName("Should handle null token in getRoleFromToken")
    void testGetRoleFromToken_NullToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.getRoleFromToken(null);
        });
    }


    @Test
    @DisplayName("Should test key padding loop when key length < 32")
    void testInitializeSecretKey_KeyPaddingLoop() {
        JwtUtil paddingUtil = new JwtUtil();
        String shortKey = "1234567890";
        ReflectionTestUtils.setField(paddingUtil, "secretKeyString", shortKey);
        
        assertDoesNotThrow(() -> paddingUtil.initializeSecretKey());
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(paddingUtil, "secretKey");
        assertNotNull(key);
        
        String token = paddingUtil.generateToken(testUserId, testEmail, testRole);
        assertNotNull(token);
    }

    @Test
    @DisplayName("Should test validateToken with token signed with different key (SignatureException)")
    void testValidateToken_SignatureException() {
        SecretKey differentKey = Keys.hmacShaKeyFor("DifferentSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong".getBytes());
        
        String tokenWithDifferentKey = Jwts.builder()
                .subject(testEmail)
                .claim("userId", testUserId.toString())
                .claim("role", testRole)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1800000))
                .signWith(differentKey)
                .compact();
        
        boolean isValid = jwtUtil.validateToken(tokenWithDifferentKey);
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should test validateToken with malformed token (MalformedJwtException)")
    void testValidateToken_MalformedJwtException() {
        String malformedToken = "not.a.valid.jwt.token.format";
        
        boolean isValid = jwtUtil.validateToken(malformedToken);
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should test validateToken with completely invalid format")
    void testValidateToken_InvalidFormat() {
        String[] invalidTokens = {
            "not-a-jwt",
            "singlepart",
            "two.parts",
            ".",
            "...",
            ""
        };
        
        for (String invalidToken : invalidTokens) {
            boolean isValid = jwtUtil.validateToken(invalidToken);
            assertFalse(isValid, "Token should be invalid: " + invalidToken);
        }
    }

    @Test
    @DisplayName("Should test isTokenExpired with token that has no expiration")
    void testIsTokenExpired_NoExpirationClaim() {
        String invalidToken = "invalid.token.here";
        boolean isExpired = jwtUtil.isTokenExpired(invalidToken);
        assertTrue(isExpired); 
    }

    @Test
    @DisplayName("Should test getExpirationDateFromToken with expired token")
    void testGetExpirationDateFromToken_ExpiredToken() {
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtUtil, "secretKey");
        
        Date pastDate = new Date(System.currentTimeMillis() - 2000);
        Date expirationDate = new Date(System.currentTimeMillis() - 1000);
        
        String expiredToken = Jwts.builder()
                .subject(testEmail)
                .claim("userId", testUserId.toString())
                .claim("role", testRole)
                .issuedAt(pastDate)
                .expiration(expirationDate)
                .signWith(key)
                .compact();
        
        assertFalse(jwtUtil.validateToken(expiredToken), "Expired token should fail validation");
        
        Date extractedExpiration = jwtUtil.getExpirationDateFromToken(expiredToken);
        
        if (extractedExpiration != null) {
            assertTrue(extractedExpiration.before(new Date()) || 
                      Math.abs(extractedExpiration.getTime() - expirationDate.getTime()) < 2000,
                      "If expiration is extracted, it should be in the past or match expected");
        }
    }

    @Test
    @DisplayName("Should test getIssuedAtDateFromToken with various tokens")
    void testGetIssuedAtDateFromToken_VariousTokens() {
        String token = jwtUtil.generateToken(testUserId, testEmail, testRole);
        Date issuedAt = jwtUtil.getIssuedAtDateFromToken(token);
        assertNotNull(issuedAt);
        
        Date invalidIssuedAt = jwtUtil.getIssuedAtDateFromToken("invalid");
        assertNull(invalidIssuedAt);
    }
}
