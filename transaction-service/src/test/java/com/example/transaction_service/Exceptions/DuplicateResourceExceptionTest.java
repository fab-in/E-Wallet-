package com.example.transaction_service.Exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateResourceExceptionTest {

    @Test
    void testDuplicateResourceException_WithMessage() {
        String message = "Duplicate resource";
        DuplicateResourceException exception = new DuplicateResourceException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testDuplicateResourceException_IsRuntimeException() {
        DuplicateResourceException exception = new DuplicateResourceException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}

