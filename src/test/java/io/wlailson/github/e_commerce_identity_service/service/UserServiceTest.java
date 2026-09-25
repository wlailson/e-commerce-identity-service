package io.wlailson.github.e_commerce_identity_service.service;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import io.wlailson.github.e_commerce_identity_service.exceptions.EntityNotFoundException;
import io.wlailson.github.e_commerce_identity_service.exceptions.UsernameNotFoundException;
import io.wlailson.github.e_commerce_identity_service.repository.UserRepository;
import io.wlailson.github.e_commerce_identity_service.repository.RoleRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User existingUser;
    private Role role;
    private long id = 1L;
    private long nonExistingId = 999L;
    private UserRequestDTO request;
    private Authentication userAuthentication;
    private Authentication adminAuthentication;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(id);
        role.setAuthority("ROLE_USER");

        existingUser = new User();
        existingUser.setId(id);
        existingUser.setName("Maria");
        existingUser.setEmail("maria@email.com");
        existingUser.setPhone("123456789");
        existingUser.setBirthDate(LocalDate.of(1990, 1, 1));
        existingUser.setPassword("oldPassword");
        existingUser.getRoles().add(role);

        request = new UserRequestDTO(
                "Maria2",
                "maria@email.com",
                LocalDate.of(1990, 1, 1),
                "123456789",
                "newpassword");

        userAuthentication = authentication("maria@email.com", "ROLE_USER");
        adminAuthentication = authentication("admin@email.com", "ROLE_ADMIN");
    }

    @Nested
    class GetUserById {

        @Test
        void shouldReturnUserWhenIdExists() {
            Mockito.when(repository.findById(id)).thenReturn(Optional.of(existingUser));

            UserResponseDTO response = service.getUserById(id);

            Assertions.assertNotNull(response);
            Assertions.assertEquals(existingUser.getId(), response.id());
            Assertions.assertEquals(existingUser.getName(), response.name());
            Assertions.assertEquals(existingUser.getEmail(), response.email());
            Assertions.assertEquals(existingUser.getPhone(), response.phone());
            Assertions.assertEquals(existingUser.getBirthDate(), response.birthDate());
            Assertions.assertTrue(response.roles().contains("ROLE_USER"));
            Mockito.verify(repository).findById(id);
        }

        @Test
        void shouldThrowEntityNotFoundExceptionWhenIdDoesNotExist() {
            Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

            Assertions.assertThrows(EntityNotFoundException.class, () -> service.getUserById(nonExistingId));
            Mockito.verify(repository).findById(nonExistingId);
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        void shouldReturnPageOfUsers() {
            UserResponseMinDTO dto = new UserResponseMinDTO(existingUser);
            Page<UserResponseMinDTO> page = new PageImpl<>(List.of(dto));
            Pageable pageable = Mockito.mock(Pageable.class);

            Mockito.when(repository.searchAllUsers(pageable)).thenReturn(page);

            Page<UserResponseMinDTO> response = service.getAllUsers(pageable);

            Assertions.assertEquals(1, response.getTotalElements());
            Assertions.assertEquals("Maria", response.getContent().get(0).name());
            Assertions.assertEquals("maria@email.com", response.getContent().get(0).email());
            Mockito.verify(repository).searchAllUsers(pageable);
        }
    }

    @Nested
    class GetUserByEmail {

        @Test
        void shouldReturnUserWhenEmailExists() {
            Mockito.when(repository.findByEmail("maria@email.com")).thenReturn(Optional.of(existingUser));

            UserResponseDTO response = service.getUserByEmail("maria@email.com");

            Assertions.assertNotNull(response);
            Assertions.assertEquals(existingUser.getEmail(), response.email());
            Assertions.assertEquals(existingUser.getName(), response.name());
            Mockito.verify(repository).findByEmail("maria@email.com");
        }

        @Test
        void shouldThrowUsernameNotFoundExceptionWhenEmailDoesNotExist() {
            Mockito.when(repository.findByEmail("not-found@email.com")).thenReturn(Optional.empty());

            Assertions.assertThrows(UsernameNotFoundException.class,
                    () -> service.getUserByEmail("not-found@email.com"));
            Mockito.verify(repository).findByEmail("not-found@email.com");
        }
    }

    @Nested
    class PostUser {

        @Test
        void shouldAssignUserRoleByDefault() {
            String encodedPassword = "encodedPassword";
            UserRequestDTO newUser = new UserRequestDTO(
                    "Joao",
                    "joao@email.com",
                    LocalDate.of(1992, 1, 1),
                    "999999999",
                    "newpassword"
            );

            Mockito.when(passwordEncoder.encode("newpassword")).thenReturn(encodedPassword);
            Mockito.when(roleRepository.findByAuthority("ROLE_USER")).thenReturn(Optional.of(role));
            Mockito.when(repository.save(Mockito.any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponseDTO response = service.postUser(newUser);

            Assertions.assertTrue(response.roles().contains("ROLE_USER"));
            Mockito.verify(repository).save(Mockito.argThat(saved ->
                    encodedPassword.equals(saved.getPassword())
                            && saved.getRoles().stream()
                            .anyMatch(savedRole -> "ROLE_USER".equals(savedRole.getAuthority()))
            ));
        }
    }

    @Nested
    class PutUser {

        @Test
        void shouldUpdateUserWhenIdExists() {
            String encodedPassword = "encodedPassword";

            Mockito.when(repository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
            Mockito.when(passwordEncoder.encode("newpassword")).thenReturn(encodedPassword);
            Mockito.when(repository.save(existingUser)).thenReturn(existingUser);

            service.putUser(existingUser.getId(), request, userAuthentication);

            Mockito.verify(repository).findById(existingUser.getId());
            Mockito.verify(passwordEncoder).encode("newpassword");
            Mockito.verify(repository).save(existingUser);

            Assertions.assertEquals("Maria2", existingUser.getName());
            Assertions.assertEquals("maria@email.com", existingUser.getEmail());
            Assertions.assertEquals(LocalDate.of(1990, 1, 1), existingUser.getBirthDate());
            Assertions.assertEquals("123456789", existingUser.getPhone());
            Assertions.assertEquals(encodedPassword, existingUser.getPassword());
        }

        @Test
        void shouldThrowEntityNotFoundExceptionWhenIdDoesNotExist() {
            Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

            Assertions.assertThrows(EntityNotFoundException.class,
                    () -> service.putUser(nonExistingId, request, userAuthentication));
            Mockito.verify(repository).findById(nonExistingId);
            Mockito.verify(passwordEncoder, Mockito.never()).encode(Mockito.anyString());
            Mockito.verify(repository, Mockito.never()).save(Mockito.any(User.class));
        }

        @Test
        void shouldNotChangePasswordWhenPasswordIsBlank() {
            UserRequestDTO user = new UserRequestDTO(
                    "Maria2",
                    "maria@email.com",
                    LocalDate.of(1990, 1, 1),
                    "123456789",
                    null);

            Mockito.when(repository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
            Mockito.when(repository.save(existingUser)).thenReturn(existingUser);

            service.putUser(existingUser.getId(), user, userAuthentication);

            Assertions.assertEquals("Maria2", existingUser.getName());
            Mockito.verify(repository).save(existingUser);
            Mockito.verify(passwordEncoder, Mockito.never()).encode(Mockito.any());
        }

        @Test
        void shouldRejectUserWhenUpdatingAnotherUser() {
            User anotherUser = new User();
            anotherUser.setId(2L);
            anotherUser.setEmail("joao@email.com");

            Mockito.when(repository.findById(anotherUser.getId())).thenReturn(Optional.of(anotherUser));

            Assertions.assertThrows(
                    org.springframework.security.access.AccessDeniedException.class,
                    () -> service.putUser(anotherUser.getId(), request, userAuthentication)
            );
            Mockito.verify(repository, Mockito.never()).save(anotherUser);
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUserWhenIdExists() {
            Mockito.when(repository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

            Assertions.assertDoesNotThrow(() -> service.deleteUser(id, adminAuthentication));

            Mockito.verify(repository).findById(id);
            Mockito.verify(repository).deleteById(id);
        }

        @Test
        void shouldThrowEntityNotFoundExceptionWhenIdDoesNotExist() {
            Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

            Assertions.assertThrows(EntityNotFoundException.class,
                    () -> service.deleteUser(nonExistingId, adminAuthentication));

            Mockito.verify(repository).findById(nonExistingId);
            Mockito.verify(repository, Mockito.never()).deleteById(nonExistingId);
        }

    }

    private Authentication authentication(String name, String authority) {
        return new UsernamePasswordAuthenticationToken(
                name,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}