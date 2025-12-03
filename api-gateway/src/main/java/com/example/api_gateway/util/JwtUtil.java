package com.example.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret:MySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm}")
    private String secretKeyString;

    private SecretKey secretKey;

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
        } catch (Exception e) {
            System.err.println("Failed to initialize JWT secret key: " + e.getMessage());
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token has expired: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token format: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            System.err.println("JWT token is null or empty: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error validating JWT token: " + e.getMessage());
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

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new java.util.Date());
        } catch (Exception e) {
            return true;
        }
    }
}

