package io.wlailson.github.e_commerce_identity_service.service;

import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(user);
    }

    public void applyRequest(User user, UserRequestDTO request) {
        user.setName(normalize(request.name()));
        user.setEmail(normalize(request.email()));
        user.setPhone(normalize(request.phone()));
        user.setBirthDate(request.birthDate());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
