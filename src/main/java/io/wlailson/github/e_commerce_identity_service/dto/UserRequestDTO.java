package io.wlailson.github.e_commerce_identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRequestDTO(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        String email,
        @NotNull(message = "BirthDate Not null")
        @Past(message = "The date must be in the past")
        LocalDate birthDate,
        @NotBlank(message = "Phone is required")
        String phone,
        @NotBlank(message = "Password is required")
        String password
        ) {
}
