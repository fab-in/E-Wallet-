package com.example.wallet_service.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @InjectMocks
    private SecurityUtil securityUtil;

    private HttpServletRequest request;
    private ServletRequestAttributes attributes;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        attributes = mock(ServletRequestAttributes.class);
    }

    @Test
    void testGetCurrentUserId_Success() {
        UUID userId = UUID.randomUUID();
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn(userId.toString());

            UUID result = securityUtil.getCurrentUserId();

            assertNotNull(result);
            assertEquals(userId, result);
        }
    }

    @Test
    void testGetCurrentUserId_NullHeader() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn(null);

            UUID result = securityUtil.getCurrentUserId();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserId_EmptyHeader() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn("");

            UUID result = securityUtil.getCurrentUserId();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserId_WhitespaceHeader() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn("   ");

            UUID result = securityUtil.getCurrentUserId();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserId_InvalidUUID() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn("invalid-uuid");

            UUID result = securityUtil.getCurrentUserId();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserId_NoRequestAttributes() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            UUID result = securityUtil.getCurrentUserId();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserRole_Success() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

            String result = securityUtil.getCurrentUserRole();

            assertEquals("ADMIN", result);
        }
    }

    @Test
    void testGetCurrentUserRole_Null() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            String result = securityUtil.getCurrentUserRole();

            assertNull(result);
        }
    }

    @Test
    void testGetCurrentUserEmail_Success() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

            String result = securityUtil.getCurrentUserEmail();

            assertEquals("test@example.com", result);
        }
    }

    @Test
    void testGetCurrentUserEmail_Null() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            String result = securityUtil.getCurrentUserEmail();

            assertNull(result);
        }
    }

    @Test
    void testIsAdmin_True() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

            boolean result = securityUtil.isAdmin();

            assertTrue(result);
        }
    }

    @Test
    void testIsAdmin_False() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Role")).thenReturn("USER");

            boolean result = securityUtil.isAdmin();

            assertFalse(result);
        }
    }

    @Test
    void testIsAdmin_NullRole() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Role")).thenReturn(null);

            boolean result = securityUtil.isAdmin();

            assertFalse(result);
        }
    }

    @Test
    void testIsAuthenticated_True() {
        UUID userId = UUID.randomUUID();
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn(userId.toString());

            boolean result = securityUtil.isAuthenticated();

            assertTrue(result);
        }
    }

    @Test
    void testIsAuthenticated_False() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getHeader("X-User-Id")).thenReturn(null);

            boolean result = securityUtil.isAuthenticated();

            assertFalse(result);
        }
    }
}

