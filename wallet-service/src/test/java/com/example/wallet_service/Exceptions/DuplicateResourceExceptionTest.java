package com.example.wallet_service.Exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateResourceExceptionTest {

    @Test
    void testConstructor() {
        String message = "Duplicate resource";
        DuplicateResourceException exception = new DuplicateResourceException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }
}

