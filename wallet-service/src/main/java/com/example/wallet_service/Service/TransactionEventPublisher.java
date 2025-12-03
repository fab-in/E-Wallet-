package com.example.wallet_service.Service;

import com.example.wallet_service.Config.RabbitMQConfig;
import com.example.wallet_service.DTO.TransactionCompletedEvent;
import com.example.wallet_service.DTO.TransactionCreatedEvent;
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

    public void publishTransactionCreated(
            UUID transactionId,
            UUID userId,
            UUID senderWalletId,
            UUID receiverWalletId,
            Double amount,
            String transactionType,
            String remarks,
            String userEmail) {

        TransactionCreatedEvent event = new TransactionCreatedEvent();
        event.setTransactionId(transactionId);
        event.setUserId(userId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(amount);
        event.setTransactionType(transactionType);
        event.setRemarks(remarks);
        event.setUserEmail(userEmail);
        event.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRANSACTION_CREATED_QUEUE,
                event);

        logger.info("Published transaction.created event: {}", transactionId);
    }

    public void publishTransactionCompleted(
            UUID transactionId,
            String status,
            String remarks) {

        TransactionCompletedEvent event = new TransactionCompletedEvent();
        event.setTransactionId(transactionId);
        event.setStatus(status);
        event.setRemarks(remarks);
        event.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRANSACTION_COMPLETED_QUEUE,
                event);

        logger.info("Published transaction.completed event: {} - {}", transactionId, status);
    }
}
