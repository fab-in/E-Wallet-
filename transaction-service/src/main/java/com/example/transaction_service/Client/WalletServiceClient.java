package com.example.transaction_service.Client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.List;
import java.util.UUID;

@Component
public class WalletServiceClient {

    private static final Logger logger = LogManager.getLogger(WalletServiceClient.class);
    private final WebClient webClient;

    public WalletServiceClient(@Value("${wallet.service.url}") String walletServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(walletServiceUrl)
                .build();
    }

    public List<WalletDTO> getUserWallets(UUID userId) {
        try {
            return webClient.get()
                    .uri("/wallets/with-balance")
                    .header("X-User-Id", userId.toString()) // Pass user ID in header
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<WalletDTO>>() {
                    })
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("HTTP Error getting user wallets: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return List.of();
            }
            return List.of();
        } catch (Exception e) {
            logger.error("Error getting user wallets: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public static class WalletDTO {
        private UUID id;
        private UUID userId;
        private String walletName;
        private String accountNumber;
        private Double balance;
        private java.time.LocalDateTime createdAt;

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getWalletName() {
            return walletName;
        }

        public void setWalletName(String walletName) {
            this.walletName = walletName;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public Double getBalance() {
            return balance;
        }

        public void setBalance(Double balance) {
            this.balance = balance;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
