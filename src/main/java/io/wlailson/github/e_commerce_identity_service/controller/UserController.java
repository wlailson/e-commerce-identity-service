package io.wlailson.github.e_commerce_identity_service.controller;

import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import io.wlailson.github.e_commerce_identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(service.getUserById(userId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponseMinDTO>> getAllUsers(
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.getAllUsers(pageable));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        return  ResponseEntity.ok(service.getUserByEmail(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> postUser(
            @RequestBody @Valid UserRequestDTO request
    ) {

        UserResponseDTO response = service.postUser(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{userId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public String login(@RequestBody UserRequestDTO request) {
        return service.login(request);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> putUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.putUser(userId, request, authentication));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        service.deleteUser(userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
