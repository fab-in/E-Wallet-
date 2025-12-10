package com.example.transaction_service.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void testSendStatementEmail_Success() throws Exception {
        byte[] csvBytes = "test,csv,content".getBytes();
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> {
            emailService.sendStatementEmail("test@example.com", "Test User", csvBytes);
        });

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendStatementEmail_MessagingException() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> {
            throw new RuntimeException(new MessagingException("Email error"));
        });

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendStatementEmail("test@example.com", "Test User", new byte[0]);
        });
        assertTrue(exception.getCause() instanceof MessagingException);
    }

    @Test
    void testSendSimpleEmail_Success() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> {
            emailService.sendSimpleEmail("test@example.com", "Subject", "Body");
        });

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendSimpleEmail_MessagingException() throws Exception {

        when(mailSender.createMimeMessage()).thenAnswer(invocation -> {
            throw new RuntimeException(new MessagingException("Email error"));
        });

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendSimpleEmail("test@example.com", "Subject", "Body");
        });
        assertTrue(exception.getCause() instanceof MessagingException);
    }

    @Test
    void testSendStatementEmail_NullUserName() throws Exception {
        byte[] csvBytes = "test,csv,content".getBytes();
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> {
            emailService.sendStatementEmail("test@example.com", null, csvBytes);
        });

        verify(mailSender).send(any(MimeMessage.class));
    }
}

