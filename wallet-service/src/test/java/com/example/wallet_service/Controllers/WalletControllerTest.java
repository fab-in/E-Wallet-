package com.example.wallet_service.Controllers;

import com.example.wallet_service.DTO.*;
import com.example.wallet_service.Service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetWallets() throws Exception {
        WalletSummaryDTO dto = new WalletSummaryDTO();
        dto.setId(UUID.randomUUID());
        when(walletService.getWallets()).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(walletService).getWallets();
    }

    @Test
    void testGetWalletsWithBalance() throws Exception {
        WalletDTO dto = new WalletDTO();
        dto.setId(UUID.randomUUID());
        when(walletService.getWalletsWithBalance()).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/wallets/with-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(walletService).getWalletsWithBalance();
    }

    @Test
    void testGetWalletById() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletDTO dto = new WalletDTO();
        dto.setId(walletId);
        when(walletService.getWalletById(walletId)).thenReturn(dto);

        mockMvc.perform(get("/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()));

        verify(walletService).getWalletById(walletId);
    }

    @Test
    void testCreateWallet() throws Exception {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(UUID.randomUUID());
        dto.setWalletName("Test Wallet");
        dto.setAccountNumber("1234567890");
        dto.setBalance(100.0);
        dto.setPasscode("1234");

        when(walletService.createWallet(any(WalletCreateDTO.class))).thenReturn(new WalletDTO());

        mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Wallet added successfully"));

        verify(walletService).createWallet(any(WalletCreateDTO.class));
    }

    @Test
    void testUpdateWallet() throws Exception {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(UUID.randomUUID().toString());
        dto.setNewWalletName("Updated Wallet");

        when(walletService.updateWallet(any(WalletUpdateDTO.class))).thenReturn(new WalletDTO());

        mockMvc.perform(put("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wallet updated successfully"));

        verify(walletService).updateWallet(any(WalletUpdateDTO.class));
    }

    @Test
    void testDeleteWallet() throws Exception {
        String walletName = "Test Wallet";
        String userIdentifier = UUID.randomUUID().toString();

        doNothing().when(walletService).deleteWallet(walletName, userIdentifier);

        mockMvc.perform(delete("/wallets")
                        .param("walletName", walletName)
                        .param("userIdentifier", userIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wallet deleted successfully"));

        verify(walletService).deleteWallet(walletName, userIdentifier);
    }

    @Test
    void testCreditWallet() throws Exception {
        UUID transactionId = UUID.randomUUID();
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(UUID.randomUUID());
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletService.creditWallet(any(CreditRequestDTO.class))).thenReturn(transactionId);

        mockMvc.perform(post("/wallets/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()));

        verify(walletService).creditWallet(any(CreditRequestDTO.class));
    }

    @Test
    void testWithdrawWallet() throws Exception {
        UUID transactionId = UUID.randomUUID();
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO();
        dto.setWalletId(UUID.randomUUID());
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletService.withdrawWallet(any(WithdrawalRequestDTO.class))).thenReturn(transactionId);

        mockMvc.perform(post("/wallets/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()));

        verify(walletService).withdrawWallet(any(WithdrawalRequestDTO.class));
    }

    @Test
    void testTransferFunds() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(UUID.randomUUID());
        dto.setDestinationWalletId(UUID.randomUUID());
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletService.transferFunds(any(TransferRequestDTO.class))).thenReturn(transactionId);

        mockMvc.perform(post("/wallets/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()));

        verify(walletService).transferFunds(any(TransferRequestDTO.class));
    }
}

