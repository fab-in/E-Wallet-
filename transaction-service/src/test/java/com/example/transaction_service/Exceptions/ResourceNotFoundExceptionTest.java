package com.example.transaction_service.Exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void testResourceNotFoundException_WithMessage() {
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testResourceNotFoundException_IsRuntimeException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}

