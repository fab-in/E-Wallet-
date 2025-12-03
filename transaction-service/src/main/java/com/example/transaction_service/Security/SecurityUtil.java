package com.example.transaction_service.Security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Security utility for extracting user information from request headers.
 * In microservices architecture, the API Gateway extracts user info from JWT
 * and passes it in request headers.
 */
@Component
public class SecurityUtil {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    /**
     * Gets the current user ID from request headers (set by API Gateway)
     */
    public UUID getCurrentUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        
        String userIdStr = request.getHeader(USER_ID_HEADER);
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets the current user role from request headers
     */
    public String getCurrentUserRole() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(USER_ROLE_HEADER);
    }

    /**
     * Gets the current user email from request headers
     */
    public String getCurrentUserEmail() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(USER_EMAIL_HEADER);
    }

    /**
     * Checks if current user is admin
     */
    public boolean isAdmin() {
        String role = getCurrentUserRole();
        return role != null && "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Checks if user is authenticated (has userId in header)
     */
    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

