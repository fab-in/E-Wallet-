package com.example.transaction_service.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private WalletServiceClient walletServiceClient;
    private UUID userId;
    private WalletServiceClient.WalletDTO walletDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletDTO = new WalletServiceClient.WalletDTO();
        walletDTO.setId(UUID.randomUUID());
        walletDTO.setUserId(userId);
        walletDTO.setWalletName("Test Wallet");
        walletDTO.setAccountNumber("1234567890");
        walletDTO.setBalance(1000.0);

        walletServiceClient = new WalletServiceClient("http://localhost:8082");
        ReflectionTestUtils.setField(walletServiceClient, "webClient", webClient);
    }

    @Test
    void testGetUserWallets_Success() {
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(wallets));

        List<WalletServiceClient.WalletDTO> result = walletServiceClient.getUserWallets(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(walletDTO.getId(), result.get(0).getId());
    }

    @Test
    void testGetUserWallets_NotFound() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

        List<WalletServiceClient.WalletDTO> result = walletServiceClient.getUserWallets(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserWallets_WebClientResponseException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(500, "Internal Server Error", null, null, null)));

        List<WalletServiceClient.WalletDTO> result = walletServiceClient.getUserWallets(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserWallets_GenericException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.error(new RuntimeException("Network error")));

        List<WalletServiceClient.WalletDTO> result = walletServiceClient.getUserWallets(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

