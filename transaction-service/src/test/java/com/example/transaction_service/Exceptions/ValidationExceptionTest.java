package com.example.transaction_service.Exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void testValidationException_WithMessage() {
        String message = "Validation error";
        ValidationException exception = new ValidationException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testValidationException_IsRuntimeException() {
        ValidationException exception = new ValidationException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}

