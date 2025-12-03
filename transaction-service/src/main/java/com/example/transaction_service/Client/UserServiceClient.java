package com.example.transaction_service.Client;

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

    public UserDTO getUserDetails(UUID userId) {
        try {
            return webClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            logger.warn("User not found: {}", userId);
            return null;
        } catch (WebClientResponseException e) {
            logger.error("HTTP Error getting user details: {} - {}", e.getStatusCode(), e.getMessage());
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
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
