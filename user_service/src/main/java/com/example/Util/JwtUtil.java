package com.example.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger logger = LogManager.getLogger(JwtUtil.class);

    private SecretKey secretKey;

    private static final long EXPIRATION_TIME = 30 * 60 * 1000; // 30 minutes

    @Value("${jwt.secret:MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm}")
    private String secretKeyString;

    @PostConstruct
    public void initializeSecretKey() {
        try {

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

            this.secretKey = Keys.hmacShaKeyFor(finalKeyBytes);

            logger.info("JWT Secret Key initialized successfully using static key from configuration");

        } catch (Exception e) {
            logger.error("Failed to initialize JWT secret key", e);
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }

    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }


    public boolean validateToken(String token) {
        try {
            
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token format: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is null or empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error validating JWT token: {}", e.getMessage(), e);
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userIdString = claims.get("userId", String.class);
        return UUID.fromString(userIdString);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; 
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public Date getIssuedAtDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getIssuedAt();
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirationTime() {
        return EXPIRATION_TIME;
    }
}
