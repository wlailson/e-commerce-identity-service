package io.wlailson.github.e_commerce_identity_service.controller;

import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import io.wlailson.github.e_commerce_identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Cadastro, consulta, atualização e autenticação de usuários")
public class UserController {

    private final UserService service;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{userId}")
    @Operation(summary = "Buscar usuário por ID", security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de administrador"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "Identificador do usuário", example = "1")
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(service.getUserById(userId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    @Operation(summary = "Listar usuários", description = "Retorna uma lista paginada. Requer o papel ROLE_ADMIN.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de usuários"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de administrador")
    })
    public ResponseEntity<Page<UserResponseMinDTO>> getAllUsers(
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(service.getAllUsers(pageable));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/me")
    @Operation(summary = "Buscar usuário autenticado", security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        return  ResponseEntity.ok(service.getUserByEmail(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Cadastrar usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
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
    @Operation(summary = "Autenticar usuário", description = "Retorna um token JWT para as credenciais informadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token JWT gerado",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public String login(@RequestBody UserRequestDTO request) {
        return service.login(request);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @PutMapping("/{userId}")
    @Operation(summary = "Atualizar usuário", description = "Administradores podem atualizar qualquer usuário; usuários comuns, apenas o próprio cadastro.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para atualizar o cadastro"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponseDTO> putUser(
            @Parameter(description = "Identificador do usuário", example = "1")
            @PathVariable Long userId,
            @RequestBody @Valid UserRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.putUser(userId, request, authentication));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{userId}")
    @Operation(summary = "Excluir usuário", security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de administrador"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Identificador do usuário", example = "1")
            @PathVariable Long userId,
            Authentication authentication
    ) {
        service.deleteUser(userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
