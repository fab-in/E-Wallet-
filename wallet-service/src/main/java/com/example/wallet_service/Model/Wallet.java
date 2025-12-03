package com.example.wallet_service.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "wallet_name")
    private String walletName;

    @Column(name = "account_number")
    private String accountNumber;

    private double balance;

    @Column(name = "passcode", length = 255)
    private String passcode; 

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
