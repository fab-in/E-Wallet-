package com.example.Security;

import com.example.Model.User;
import com.example.Repository.UserRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtil Unit Tests")
class SecurityUtilTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityUtil securityUtil;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns null when no authentication present")
    void getCurrentUser_NoAuthentication() {
        SecurityContextHolder.clearContext();

        assertNull(securityUtil.getCurrentUser());
    }

    @Test
    @DisplayName("Returns null when authentication not authenticated")
    void getCurrentUser_NotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.setContext(securityContext);

        assertNull(securityUtil.getCurrentUser());
    }

    @Test
    @DisplayName("Returns principal when authentication contains User")
    void getCurrentUser_PrincipalUser() {
        User user = buildUser("USER");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        User result = securityUtil.getCurrentUser();

        assertEquals(user, result);
    }

    @Test
    @DisplayName("Fetches user by email when principal is String")
    void getCurrentUser_PrincipalEmailFound() {
        User user = buildUser("ADMIN");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);

        User result = securityUtil.getCurrentUser();

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    @DisplayName("Returns null when principal email not found")
    void getCurrentUser_PrincipalEmailMissing() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("missing@example.com");
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        SecurityContextHolder.setContext(securityContext);

        assertNull(securityUtil.getCurrentUser());
    }

    @Test
    @DisplayName("Returns user id when user present")
    void getCurrentUserId_UserPresent() {
        User user = buildUser("USER");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertEquals(user.getId(), securityUtil.getCurrentUserId());
    }

    @Test
    @DisplayName("Returns null user id when no user")
    void getCurrentUserId_NoUser() {
        assertNull(securityUtil.getCurrentUserId());
    }

    @Test
    @DisplayName("Detects admin role irrespective of case")
    void isAdmin_ReturnsTrueForAdmin() {
        User admin = buildUser("admin");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(admin);
        SecurityContextHolder.setContext(securityContext);

        assertTrue(securityUtil.isAdmin());
    }

    @Test
    @DisplayName("hasRole checks expected role ignoring case")
    void hasRole_MatchesRole() {
        User user = buildUser("User");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertTrue(securityUtil.hasRole("user"));
        assertFalse(securityUtil.hasRole("admin"));
    }

    @Test
    @DisplayName("Returns null when principal is neither User nor String")
    void getCurrentUser_PrincipalOtherType() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(12345); // Integer, not User or String
        SecurityContextHolder.setContext(securityContext);

        assertNull(securityUtil.getCurrentUser());
    }

    @Test
    @DisplayName("Returns null when authentication is null")
    void getCurrentUser_AuthenticationNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertNull(securityUtil.getCurrentUser());
    }

    @Test
    @DisplayName("isAdmin returns false when user is null")
    void isAdmin_NoUser() {
        assertFalse(securityUtil.isAdmin());
    }

    @Test
    @DisplayName("isAdmin returns false when user role is not ADMIN")
    void isAdmin_NotAdmin() {
        User user = buildUser("USER");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertFalse(securityUtil.isAdmin());
    }

    @Test
    @DisplayName("isAdmin returns false when user role is null")
    void isAdmin_NullRole() {
        User user = buildUser(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertFalse(securityUtil.isAdmin());
    }

    @Test
    @DisplayName("hasRole returns false when role parameter is null")
    void hasRole_NullRoleParameter() {
        User user = buildUser("USER");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertFalse(securityUtil.hasRole(null));
    }

    @Test
    @DisplayName("hasRole returns false when user is null")
    void hasRole_NoUser() {
        assertFalse(securityUtil.hasRole("USER"));
    }

    @Test
    @DisplayName("hasRole returns false when user role is null")
    void hasRole_UserRoleNull() {
        User user = buildUser(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        assertFalse(securityUtil.hasRole("USER"));
    }

    private User buildUser(String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@example.com");
        user.setRole(role);
        return user;
    }
}
