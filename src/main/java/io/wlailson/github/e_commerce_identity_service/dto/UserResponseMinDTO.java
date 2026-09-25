package io.wlailson.github.e_commerce_identity_service.dto;

import io.wlailson.github.e_commerce_identity_service.domain.User;

public record UserResponseMinDTO(Long id, String name, String email) {

    public UserResponseMinDTO(User entity) {
        this(entity.getId(), entity.getName(), entity.getEmail());
    }
}
