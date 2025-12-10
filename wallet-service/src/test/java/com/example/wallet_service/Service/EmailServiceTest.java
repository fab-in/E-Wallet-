package com.example.wallet_service.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSendStatementEmail() throws MessagingException {
        byte[] csvBytes = "Transaction,Amount\nCredit,100.0".getBytes(StandardCharsets.UTF_8);
        
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendStatementEmail("test@example.com", "Test User", csvBytes));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendStatementEmail_NullUserName() throws MessagingException {
        byte[] csvBytes = "Transaction,Amount\nCredit,100.0".getBytes(StandardCharsets.UTF_8);
        
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendStatementEmail("test@example.com", null, csvBytes));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendSimpleEmail() throws MessagingException {
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendSimpleEmail("test@example.com", "Test Subject", "Test Body"));

        verify(mailSender).send(any(MimeMessage.class));
    }
}

