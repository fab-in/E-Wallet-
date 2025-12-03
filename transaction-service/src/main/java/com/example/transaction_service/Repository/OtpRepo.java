package com.example.transaction_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.transaction_service.Model.Otp;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepo extends JpaRepository<Otp, UUID> {
    
    Optional<Otp> findByTransactionId(UUID transactionId);
    
    Optional<Otp> findByTransactionIdAndIsVerifiedFalse(UUID transactionId);
}


