package com.example.Exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    void testHandleResourceNotFoundException() {
        String message = "User not found with id: 123";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        ResponseEntity<String> response = globalExceptionHandler.handleResourceNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with 409 status")
    void testHandleDuplicateResourceException() {
        String message = "User with email already exists";
        DuplicateResourceException exception = new DuplicateResourceException(message);

        ResponseEntity<String> response = globalExceptionHandler.handleDuplicateResourceException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    @Test
    @DisplayName("Should handle ValidationException with 400 status")
    void testHandleValidationException() {
        String message = "Email is required";
        ValidationException exception = new ValidationException(message);

        ResponseEntity<String> response = globalExceptionHandler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void testHandleIllegalArgumentException() {
        String message = "Invalid argument provided";
        IllegalArgumentException exception = new IllegalArgumentException(message);

        ResponseEntity<String> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with field errors")
    void testHandleValidationExceptions() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        List<org.springframework.validation.ObjectError> errors = new ArrayList<>();
        FieldError fieldError1 = new FieldError("userCreateDTO", "email", "Email is required");
        FieldError fieldError2 = new FieldError("userCreateDTO", "password", "Password must be at least 6 characters");
        errors.add(fieldError1);
        errors.add(fieldError2);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(errors);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Email is required", response.getBody().get("email"));
        assertEquals("Password must be at least 6 characters", response.getBody().get("password"));
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 status")
    void testHandleGlobalException() {
        String message = "Database connection failed";
        Exception exception = new Exception(message);

        ResponseEntity<String> response = globalExceptionHandler.handleGlobalException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("An unexpected error occurred"));
        assertTrue(response.getBody().contains(message));
    }

    @Test
    @DisplayName("Should handle Exception with null message")
    void testHandleGlobalException_NullMessage() {
        Exception exception = new Exception();

        ResponseEntity<String> response = globalExceptionHandler.handleGlobalException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
