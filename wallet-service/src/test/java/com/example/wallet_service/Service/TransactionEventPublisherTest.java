package com.example.wallet_service.Service;

import com.example.wallet_service.Config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TransactionEventPublisher transactionEventPublisher;

    private UUID transactionId;
    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();
    }

    @Test
    void testPublishTransactionCreated() {
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

        transactionEventPublisher.publishTransactionCreated(
                transactionId,
                userId,
                senderWalletId,
                receiverWalletId,
                100.0,
                "CREDIT",
                "Test transaction",
                "test@example.com"
        );

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.TRANSACTION_CREATED_QUEUE), any(Object.class));
    }

    @Test
    void testPublishTransactionCompleted() {
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

        transactionEventPublisher.publishTransactionCompleted(
                transactionId,
                "SUCCESS",
                "Transaction completed"
        );

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.TRANSACTION_COMPLETED_QUEUE), any(Object.class));
    }
}

