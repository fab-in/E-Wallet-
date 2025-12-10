package com.example.transaction_service.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SecurityUtil securityUtil;

    private UUID userId;
    private String userIdStr;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userIdStr = userId.toString();
        
        RequestContextHolder.setRequestAttributes(requestAttributes);
        lenient().when(requestAttributes.getRequest()).thenReturn(request);
    }

    @Test
    void testGetCurrentUserId_Success() {
        when(request.getHeader("X-User-Id")).thenReturn(userIdStr);

        UUID result = securityUtil.getCurrentUserId();

        assertNotNull(result);
        assertEquals(userId, result);
    }

    @Test
    void testGetCurrentUserId_NullHeader() {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        UUID result = securityUtil.getCurrentUserId();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserId_EmptyHeader() {
        when(request.getHeader("X-User-Id")).thenReturn("   ");

        UUID result = securityUtil.getCurrentUserId();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserId_InvalidUUID() {
        when(request.getHeader("X-User-Id")).thenReturn("invalid-uuid");

        UUID result = securityUtil.getCurrentUserId();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserId_NullRequest() {
        RequestContextHolder.setRequestAttributes(null);

        UUID result = securityUtil.getCurrentUserId();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserRole_Success() {
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

        String result = securityUtil.getCurrentUserRole();

        assertEquals("ADMIN", result);
    }

    @Test
    void testGetCurrentUserRole_NullHeader() {
        when(request.getHeader("X-User-Role")).thenReturn(null);

        String result = securityUtil.getCurrentUserRole();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserRole_NullRequest() {
        RequestContextHolder.setRequestAttributes(null);

        String result = securityUtil.getCurrentUserRole();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserEmail_Success() {
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        String result = securityUtil.getCurrentUserEmail();

        assertEquals("test@example.com", result);
    }

    @Test
    void testGetCurrentUserEmail_NullHeader() {
        when(request.getHeader("X-User-Email")).thenReturn(null);

        String result = securityUtil.getCurrentUserEmail();

        assertNull(result);
    }

    @Test
    void testGetCurrentUserEmail_NullRequest() {
        RequestContextHolder.setRequestAttributes(null);

        String result = securityUtil.getCurrentUserEmail();

        assertNull(result);
    }

    @Test
    void testIsAdmin_True() {
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

        boolean result = securityUtil.isAdmin();

        assertTrue(result);
    }

    @Test
    void testIsAdmin_Lowercase() {
        when(request.getHeader("X-User-Role")).thenReturn("admin");

        boolean result = securityUtil.isAdmin();

        assertTrue(result);
    }

    @Test
    void testIsAdmin_False() {
        when(request.getHeader("X-User-Role")).thenReturn("USER");

        boolean result = securityUtil.isAdmin();

        assertFalse(result);
    }

    @Test
    void testIsAdmin_NullRole() {
        when(request.getHeader("X-User-Role")).thenReturn(null);

        boolean result = securityUtil.isAdmin();

        assertFalse(result);
    }

    @Test
    void testIsAuthenticated_True() {
        when(request.getHeader("X-User-Id")).thenReturn(userIdStr);

        boolean result = securityUtil.isAuthenticated();

        assertTrue(result);
    }

    @Test
    void testIsAuthenticated_False() {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        boolean result = securityUtil.isAuthenticated();

        assertFalse(result);
    }
}

