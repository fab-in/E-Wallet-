package com.example.transaction_service.Controllers;

import com.example.transaction_service.DTO.MessageResponseDTO;
import com.example.transaction_service.DTO.OtpVerificationDTO;
import com.example.transaction_service.DTO.PaginatedResponse;
import com.example.transaction_service.DTO.TransactionDTO;
import com.example.transaction_service.Service.OtpService;
import com.example.transaction_service.Service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OtpService otpService;

    // Default pagination values
    private static final int DEFAULT_PAGE = 0; // First page (0-indexed)
    private static final int DEFAULT_SIZE = 20; // 20 items per page

    @GetMapping("/transactions")
    public ResponseEntity<PaginatedResponse<TransactionDTO>> getTransactions(
            @RequestParam(name = "type", required = false, defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order) {

        Pageable pageable = createPageable(page, size, sort, order);

        PaginatedResponse<TransactionDTO> response = transactionService.getTransactions(type, pageable);
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, String sort, String order) {

        if (page < 0) {
            page = DEFAULT_PAGE;
        }

        if (size < 1) {
            size = DEFAULT_SIZE;
        }

        if (order != null && !order.trim().isEmpty()) {
            Sort.Direction sortDirection = "oldest".equalsIgnoreCase(order.trim())
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC; // "newest" or default = DESC

            Sort sortObj = Sort.by(sortDirection, "transactionDate");
            return PageRequest.of(page, size, sortObj);
        }

        if (sort != null && !sort.trim().isEmpty()) {

            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                String field = sortParts[0].trim();
                String direction = sortParts[1].trim().toLowerCase();

                Sort.Direction sortDirection = "desc".equals(direction)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                Sort sortObj = Sort.by(sortDirection, field);

                return PageRequest.of(page, size, sortObj);
            }
        }

        // Default sorting: transactionDate DESC
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "transactionDate");
        return PageRequest.of(page, size, defaultSort);
    }

    @GetMapping("/transactions/statement")
    public ResponseEntity<MessageResponseDTO> getStatement() {
        try {
            transactionService.generateAndEmailStatement();

            MessageResponseDTO response = new MessageResponseDTO();
            response.setMessage("Transaction statement has been sent to your registered email address");
            return ResponseEntity.ok(response);
        } catch (jakarta.mail.MessagingException e) {
            MessageResponseDTO response = new MessageResponseDTO();
            response.setMessage("Failed to send email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/transactions/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody OtpVerificationDTO otpVerificationDTO) {
        try {

            boolean isVerified = otpService.verifyOtp(
                    otpVerificationDTO.getTransactionId(),
                    otpVerificationDTO.getOtp());

            if (isVerified) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "OTP verified successfully. Transaction is being processed.");
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }
        } catch (com.example.transaction_service.Exceptions.ValidationException e) {
            Map<String, String> response = new HashMap<>();
            if (e.getMessage().contains("Transaction has failed")) {
                response.put("message", e.getMessage());
                response.put("status", "failed");
                return ResponseEntity.badRequest().body(response);
            } else {
                response.put("message", e.getMessage());
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP verification failed");
        response.put("status", "failed");
        return ResponseEntity.badRequest().body(response);
    }
}
