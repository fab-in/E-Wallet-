package com.example.Security;

import com.example.Model.User;
import com.example.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class SecurityUtil {

    @Autowired
    private UserRepo userRepo;

    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof User) {
            return (User) principal;
        }
        
        if (principal instanceof String) {
            String email = (String) principal;
            Optional<User> userOptional = userRepo.findByEmail(email);
            return userOptional.orElse(null);
        }
        
        return null;
    }

    public java.util.UUID getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public boolean hasRole(String role) {
        User user = getCurrentUser();
        return user != null && role != null && role.equalsIgnoreCase(user.getRole());
    }
}

