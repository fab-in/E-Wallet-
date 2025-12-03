package com.example.transaction_service.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO for OTP verification request")
public class OtpVerificationDTO {
    
    @NotNull(message = "Transaction ID is required")
    @Schema(description = "Transaction ID received from credit/withdraw/transfer endpoint", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID transactionId;
    
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    @Schema(description = "6-digit OTP code received via email", example = "123456", minLength = 6, maxLength = 6)
    private String otp;
}

