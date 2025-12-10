package com.example.transaction_service.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private UserServiceClient userServiceClient;
    private UUID userId;
    private UserServiceClient.UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDTO = new UserServiceClient.UserDTO();
        userDTO.setId(userId);
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");

        userServiceClient = new UserServiceClient("http://localhost:8081");
        ReflectionTestUtils.setField(userServiceClient, "webClient", webClient);
    }

    @Test
    void testGetUserDetails_Success() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(UUID.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserServiceClient.UserDTO.class)).thenReturn(Mono.just(userDTO));

        UserServiceClient.UserDTO result = userServiceClient.getUserDetails(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetUserDetails_NotFound() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(UUID.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserServiceClient.UserDTO.class))
            .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

        UserServiceClient.UserDTO result = userServiceClient.getUserDetails(userId);

        assertNull(result);
    }

    @Test
    void testGetUserDetails_WebClientResponseException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(UUID.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserServiceClient.UserDTO.class))
            .thenReturn(Mono.error(WebClientResponseException.create(500, "Internal Server Error", null, null, null)));

        UserServiceClient.UserDTO result = userServiceClient.getUserDetails(userId);

        assertNull(result);
    }

    @Test
    void testGetUserDetails_GenericException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(UUID.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserServiceClient.UserDTO.class))
            .thenReturn(Mono.error(new RuntimeException("Network error")));

        UserServiceClient.UserDTO result = userServiceClient.getUserDetails(userId);

        assertNull(result);
    }
}

