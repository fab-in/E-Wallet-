package com.example.transaction_service.Service;

import com.example.transaction_service.Config.RabbitMQConfig;
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
    void testPublishOtpVerified_Success() {
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

        transactionEventPublisher.publishOtpVerified(
            transactionId,
            userId,
            senderWalletId,
            receiverWalletId,
            100.0,
            "TRANSFER"
        );

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OTP_VERIFIED_QUEUE), any(Object.class));
    }

    @Test
    void testPublishOtpVerified_WithDifferentTransactionType() {
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

        transactionEventPublisher.publishOtpVerified(
            transactionId,
            userId,
            senderWalletId,
            receiverWalletId,
            200.0,
            "CREDIT"
        );

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OTP_VERIFIED_QUEUE), any(Object.class));
    }
}

