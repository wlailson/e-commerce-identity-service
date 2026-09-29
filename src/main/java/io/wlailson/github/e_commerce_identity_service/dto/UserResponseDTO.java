package io.wlailson.github.e_commerce_identity_service.dto;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados completos de um usuário")
public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        @Schema(description = "Papéis atribuídos ao usuário", example = "[\"ROLE_USER\"]")
        List <String> roles

) {

    public  UserResponseDTO(User entity) {
        this(entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getBirthDate(),
                entity.getRoles().stream().map(Role::getAuthority).toList()

        );
    }
}
