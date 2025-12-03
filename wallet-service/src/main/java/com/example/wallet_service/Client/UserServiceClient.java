package com.example.wallet_service.Client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.UUID;

@Component
public class UserServiceClient {

    private static final Logger logger = LogManager.getLogger(UserServiceClient.class);
    private final WebClient webClient;

    public UserServiceClient(@Value("${user.service.url}") String userServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    public boolean validateUser(UUID userId) {
        try {
            UserDTO user = webClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
            return user != null && user.getId() != null;
        } catch (WebClientResponseException.NotFound e) {
            // User not found - this is expected for validation
            return false;
        } catch (WebClientResponseException e) {
            // Other HTTP errors (5xx, etc.)
            logger.warn("HTTP Error validating user: {} - {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            // Network errors, timeouts, etc.
            logger.error("Error validating user: {}", e.getMessage(), e);
            return false;
        }
    }

   
    public String getUserEmail(UUID userId) {
        try {
            UserDTO user = webClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
            return user != null ? user.getEmail() : null;
        } catch (WebClientResponseException.NotFound e) {
            // User not found
            return null;
        } catch (Exception e) {
            logger.error("Error getting user email: {}", e.getMessage(), e);
            return null;
        }
    }

    
    public UserDTO getUserDetails(UUID userId) {
        try {
            return webClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        } catch (Exception e) {
            logger.error("Error getting user details: {}", e.getMessage(), e);
            return null;
        }
    }

    
    public static class UserDTO {
        private UUID id;
        private String name;
        private String email;
        
        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}

