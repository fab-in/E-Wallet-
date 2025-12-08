package com.example.Security;

import com.example.Model.User;
import com.example.Repository.UserRepo;
import com.example.Util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepo userRepo;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Skips authentication when header missing")
    void doFilter_NoHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userRepo);
    }

    @Test
    @DisplayName("Logs and skips when header without Bearer")
    void doFilter_HeaderWithoutBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Token abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userRepo);
    }

    @Test
    @DisplayName("Skips authentication when email extraction fails")
    void doFilter_EmailExtractionFails() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtUtil.getEmailFromToken("invalid")).thenThrow(new RuntimeException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).getEmailFromToken("invalid");
        verifyNoInteractions(userRepo);
    }

    @Test
    @DisplayName("Sets authentication when token valid and user found")
    void doFilter_ValidTokenSetsAuthentication() throws ServletException, IOException {
        String token = "valid.token";
        User user = buildUser("USER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getEmailFromToken(token)).thenReturn(user.getEmail());
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Does not authenticate when token invalid")
    void doFilter_InvalidToken() throws ServletException, IOException {
        String token = "bad.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getEmailFromToken(token)).thenReturn("john@example.com");
        when(jwtUtil.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Does not authenticate when user not found")
    void doFilter_UserNotFound() throws ServletException, IOException {
        String token = "valid.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getEmailFromToken(token)).thenReturn("john@example.com");
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private User buildUser(String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john@example.com");
        user.setRole(role);
        return user;
    }
}
