package com.example.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {
    @NotBlank(message = "Email is required and cannot be empty")
    private String email;

    @NotBlank(message = "Password is required and cannot be empty")
    private String password;
}

