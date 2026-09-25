package io.wlailson.github.e_commerce_identity_service.service;

import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import io.wlailson.github.e_commerce_identity_service.exceptions.EntityNotFoundException;
import io.wlailson.github.e_commerce_identity_service.exceptions.UsernameNotFoundException;
import io.wlailson.github.e_commerce_identity_service.repository.UserRepository;
import io.wlailson.github.e_commerce_identity_service.repository.RoleRepository;
import io.wlailson.github.e_commerce_identity_service.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        return toResponse(loadEntityById(userId));
    }

    @Transactional(readOnly = true)
    public Page<UserResponseMinDTO> getAllUsers(Pageable pageable) {
        return repository.searchAllUsers(pageable);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        return toResponse(loadUserByEmail(email));
    }

    @Transactional
    public UserResponseDTO postUser(UserRequestDTO request) {

        User user = new User();

        applyRequest(user, request);
        user.getRoles().add(
                roleRepository.findByAuthority("ROLE_USER")
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: ROLE_USER"))
        );

        user.setPassword(passwordEncoder.encode(request.password()));

        return toResponse(repository.save(user));
    }

    @Transactional
    public UserResponseDTO putUser(
            Long userId,
            UserRequestDTO request,
            Authentication authentication
    ) {
        User user = loadEntityById(userId);
        assertCanModify(user, authentication);

        applyRequest(user, request);

        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return toResponse(repository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, Authentication authentication) {

        User user = loadEntityById(userId);
        assertCanDelete(user, authentication);

        repository.deleteById(user.getId());
    }

    @Transactional
    public String login(UserRequestDTO dto) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                dto.email(),
                                dto.password()
                        )
                );
        User user = (User) authentication.getPrincipal();

        return jwtService.generateToken(user);
    }

    private User loadUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    private User loadEntityById(Long userId) {
        return repository.findById(userId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Usuário não encontrado: " + userId));
    }

    private void assertCanModify(User user, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return;
        }

        if (hasAuthority(authentication, "ROLE_USER")
                && authentication.getName().equals(user.getEmail())) {
            return;
        }

        throw new AccessDeniedException("Usuário não autorizado a modificar este recurso");
    }

    private void assertCanDelete(User user, Authentication authentication) {
        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Usuário não autorizado a excluir este recurso");
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private void applyRequest(User user, UserRequestDTO request) {
        user.setName(request.name());

        user.setEmail(request.email());

        user.setPhone(request.phone());

        user.setBirthDate(request.birthDate());

    }

    private UserResponseDTO toResponse(User user) {

        return new UserResponseDTO(user);
    }
}
