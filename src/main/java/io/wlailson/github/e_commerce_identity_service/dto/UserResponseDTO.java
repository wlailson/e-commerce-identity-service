package io.wlailson.github.e_commerce_identity_service.dto;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;

import java.time.LocalDate;
import java.util.List;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
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
