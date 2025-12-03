package com.example.wallet_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.wallet_service.Model.Wallet;
import java.util.List;
import java.util.UUID;

public interface WalletRepo extends JpaRepository<Wallet, UUID> {
    boolean existsByAccountNumber(String accountNumber);
    List<Wallet> findByUserId(UUID userId);
}

