package com.example.wallet_service.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.wallet_service.Service.WalletService;
import com.example.wallet_service.DTO.WalletDTO;
import com.example.wallet_service.DTO.WalletCreateDTO;
import com.example.wallet_service.DTO.WalletUpdateDTO;
import com.example.wallet_service.DTO.CreditRequestDTO;
import com.example.wallet_service.DTO.WithdrawalRequestDTO;
import com.example.wallet_service.DTO.TransferRequestDTO;
import com.example.wallet_service.DTO.WalletSummaryDTO;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/wallets")
    public ResponseEntity<List<WalletSummaryDTO>> getWallets() {
        List<WalletSummaryDTO> wallets = walletService.getWallets();
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/wallets/with-balance")
    public ResponseEntity<List<WalletDTO>> getWalletsWithBalance() {
        List<WalletDTO> wallets = walletService.getWalletsWithBalance();
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/wallets/{id}")
    public ResponseEntity<WalletDTO> getWalletById(@PathVariable UUID id) {
        WalletDTO walletDTO = walletService.getWalletById(id);
        return ResponseEntity.ok(walletDTO);
    }

    @PostMapping("/wallets")
    public ResponseEntity<Map<String, String>> createWallet(@Valid @RequestBody WalletCreateDTO walletCreateDTO) {
        walletService.createWallet(walletCreateDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wallet added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/wallets")
    public ResponseEntity<Map<String, String>> updateWallet(@Valid @RequestBody WalletUpdateDTO walletUpdateDTO) {
        walletService.updateWallet(walletUpdateDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wallet updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/wallets")
    public ResponseEntity<Map<String, String>> deleteWallet(
            @RequestParam String walletName,
            @RequestParam String userIdentifier) {
        walletService.deleteWallet(walletName, userIdentifier);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wallet deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/wallets/credit")
    public ResponseEntity<Map<String, Object>> creditWallet(@Valid @RequestBody CreditRequestDTO creditRequestDTO) {
        UUID transactionId = walletService.creditWallet(creditRequestDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP has been sent to your email. Please verify to complete the transaction.");
        response.put("transactionId", transactionId.toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/wallets/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawWallet(
            @Valid @RequestBody WithdrawalRequestDTO withdrawalRequestDTO) {
        UUID transactionId = walletService.withdrawWallet(withdrawalRequestDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP has been sent to your email. Please verify to complete the transaction.");
        response.put("transactionId", transactionId.toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/wallets/transfer")
    public ResponseEntity<Map<String, Object>> transferFunds(@Valid @RequestBody TransferRequestDTO transferRequestDTO) {
        UUID transactionId = walletService.transferFunds(transferRequestDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP has been sent to your email. Please verify to complete the transaction.");
        response.put("transactionId", transactionId.toString());
        return ResponseEntity.ok(response);
    }
}
