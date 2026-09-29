package io.wlailson.github.e_commerce_identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record UserRequestDTO(
        @NotBlank(message = "Name is required")
        @Schema(description = "Nome completo do usuário", example = "Maria Silva")
        String name,
        @NotBlank(message = "Email is required")
        @Schema(description = "E-mail usado para autenticação", example = "maria@example.com")
        String email,
        @NotNull(message = "BirthDate Not null")
        @Past(message = "The date must be in the past")
        @Schema(description = "Data de nascimento", example = "1990-05-20")
        LocalDate birthDate,
        @NotBlank(message = "Phone is required")
        @Schema(description = "Telefone de contato", example = "61999999999")
        String phone,
        @NotBlank(message = "Password is required")
        @Schema(description = "Senha do usuário", example = "secret123", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password
        ) {
}
