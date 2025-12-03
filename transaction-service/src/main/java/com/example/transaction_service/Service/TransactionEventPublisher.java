package com.example.transaction_service.Service;

import com.example.transaction_service.Config.RabbitMQConfig;
import com.example.transaction_service.DTO.OtpVerifiedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionEventPublisher {

    private static final Logger logger = LogManager.getLogger(TransactionEventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishOtpVerified(
            UUID transactionId,
            UUID userId,
            UUID senderWalletId,
            UUID receiverWalletId,
            Double amount,
            String transactionType) {
        
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setUserId(userId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(amount);
        event.setTransactionType(transactionType);
        event.setTimestamp(System.currentTimeMillis());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.OTP_VERIFIED_QUEUE,
            event
        );
        
        logger.info("Published otp.verified event: {}", transactionId);
    }
}

