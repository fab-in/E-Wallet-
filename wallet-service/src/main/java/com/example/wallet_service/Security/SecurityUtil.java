package com.example.wallet_service.Security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class SecurityUtil {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

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

    public String getCurrentUserRole() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(USER_ROLE_HEADER);
    }

    public String getCurrentUserEmail() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(USER_EMAIL_HEADER);
    }

    public boolean isAdmin() {
        String role = getCurrentUserRole();
        return role != null && "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}


