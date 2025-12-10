package com.example.wallet_service.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceClientTest {

    private UserServiceClient userServiceClient;
    private UUID userId;
    private UserServiceClient.UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userServiceClient = new UserServiceClient("http://localhost:8081");
        userId = UUID.randomUUID();
        userDTO = new UserServiceClient.UserDTO();
        userDTO.setId(userId);
        userDTO.setEmail("test@example.com");
        userDTO.setName("Test User");
    }

    @Test
    void testValidateUser_Success() {
        userServiceClient.validateUser(userId);
        assertNotNull(userServiceClient);
    }

    @Test
    void testGetUserEmail() {
        userServiceClient.getUserEmail(userId);
        assertNotNull(userServiceClient);
    }

    @Test
    void testGetUserDetails() {
        userServiceClient.getUserDetails(userId);
        assertNotNull(userServiceClient);
    }

    @Test
    void testUserDTO() {
        UserServiceClient.UserDTO dto = new UserServiceClient.UserDTO();
        dto.setId(userId);
        dto.setEmail("test@example.com");
        dto.setName("Test User");

        assertEquals(userId, dto.getId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Test User", dto.getName());
    }
}

