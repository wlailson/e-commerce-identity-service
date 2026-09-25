package io.wlailson.github.e_commerce_identity_service.controller;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.repository.RoleRepository;
import io.wlailson.github.e_commerce_identity_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class UserControllerIT extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "maria@email.com";
    private static final String USER_PASSWORD = "123456";
    private static final String ADMIN_EMAIL = "admin@email.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private User admin;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        user = saveUser("Maria", USER_EMAIL, USER_PASSWORD, "61996695660", saveRole("ROLE_USER"));
        admin = saveUser("Admin", ADMIN_EMAIL, ADMIN_PASSWORD, "61111111111", saveRole("ROLE_ADMIN"));

        userToken = login(USER_EMAIL, USER_PASSWORD);
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Nested
    class Login {

        @Test
        void shouldReturnTokenWithValidCredentials() {
            assertThat(userToken).isNotBlank();
        }

        @Test
        void shouldRejectInvalidPassword() {
            assertThat(loginExpectingUnauthorized(USER_EMAIL, "invalid-password")).isNull();
        }

        @Test
        void shouldRejectUnknownUser() {
            assertThat(loginExpectingUnauthorized("unknown@email.com", USER_PASSWORD)).isNull();
        }
    }

    @Nested
    class GetUserById {

        @Test
        void shouldReturnUserForAdmin() {
            client.get()
                    .uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(user.getId().intValue())
                    .jsonPath("$.email").isEqualTo(USER_EMAIL)
                    .jsonPath("$.roles[0]").isEqualTo("ROLE_USER");
        }

        @Test
        void shouldRejectUnauthenticatedRequest() {
            client.get().uri("/users/{id}", user.getId())
                    .exchange().expectStatus().isUnauthorized();
        }

        @Test
        void shouldRejectRegularUserUpdatingAnotherUser() {
            client.put().uri("/users/{id}", admin.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson("Admin changed", ADMIN_EMAIL, "61111111111", "new-password"))
                    .exchange().expectStatus().isForbidden();
        }

        @Test
        void shouldRejectRegularUser() {
            client.get().uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                    .exchange().expectStatus().isForbidden();
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        void shouldReturnPagedUsersForAdmin() {
            client.get().uri("/users?page=0&size=10")
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .exchange().expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(2)
                    .jsonPath("$.totalElements").isEqualTo(2);
        }

        @Test
        void shouldRejectRegularUser() {
            client.get().uri("/users")
                    .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                    .exchange().expectStatus().isForbidden();
        }
    }

    @Nested
    class GetCurrentUser {

        @Test
        void shouldReturnAuthenticatedUser() {
            client.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(user.getId().intValue())
                    .jsonPath("$.email").isEqualTo(USER_EMAIL);
        }

        @Test
        void shouldRejectUnauthenticatedRequest() {
            client.get().uri("/users/me")
                    .exchange().expectStatus().isUnauthorized();
        }
    }

    @Nested
    class PostUser {

        @Test
        void shouldCreateUserWithoutAuthentication() {
            String email = "joao@email.com";

            client.post().uri("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson("Joao", email, "999999999", "new-password"))
                    .exchange().expectStatus().isCreated()
                    .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*/users/[0-9]+")
                    .expectBody().jsonPath("$.email").isEqualTo(email);

            User saved = userRepository.findByEmail(email).orElseThrow();
            assertThat(passwordEncoder.matches("new-password", saved.getPassword())).isTrue();
            assertThat(saved.getRoles())
                    .extracting(Role::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    class PutUser {

        @Test
        void shouldUpdateUserForAdmin() {
            client.put().uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson("Maria Silva", USER_EMAIL, "61990000000", "updated-password"))
                    .exchange().expectStatus().isOk()
                    .expectBody().jsonPath("$.name").isEqualTo("Maria Silva");

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches("updated-password", updated.getPassword())).isTrue();
        }

        @Test
        void shouldRejectNullPassword() {
            client.put().uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJsonWithNullPassword())
                    .exchange().expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("Um ou mais campos são inválidos")
                    .jsonPath("$.errors.password").isEqualTo("Password is required");
        }

        @Test
        void shouldRejectUnauthenticatedRequest() {
            client.put().uri("/users/{id}", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson("Maria Silva", USER_EMAIL, "61990000000", "updated-password"))
                    .exchange().expectStatus().isUnauthorized();
        }

        @Test
        void shouldReturnNotFoundForUnknownUser() {
            client.put().uri("/users/{id}", 999999L)
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userJson("Maria Silva", USER_EMAIL, "61990000000", "updated-password"))
                    .exchange().expectStatus().isNotFound();
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUserForAdmin() {
            client.delete().uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .exchange().expectStatus().isNoContent();

            assertThat(userRepository.findById(user.getId())).isEmpty();
        }

        @Test
        void shouldRejectRegularUser() {
            client.delete().uri("/users/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                    .exchange().expectStatus().isForbidden();
        }

        @Test
        void shouldReturnNotFoundForUnknownUser() {
            client.delete().uri("/users/{id}", 999999L)
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .exchange().expectStatus().isNotFound();
        }
    }

    private Role saveRole(String authority) {
        Role role = new Role();
        role.setAuthority(authority);
        return roleRepository.save(role);
    }

    private User saveUser(String name, String email, String password, String phone, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setBirthDate(LocalDate.of(1990, 8, 20));
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private String login(String email, String password) {
        return client.post().uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginJson(email, password))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private String loginExpectingUnauthorized(String email, String password) {
        return client.post().uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginJson(email, password))
                .exchange().expectStatus().isUnauthorized()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String userJson(String name, String email, String phone, String password) {
        return """
                {"name":"%s","email":"%s","birthDate":"1990-08-20","phone":"%s","password":"%s"}
                """.formatted(name, email, phone, password);
    }

    private String userJsonWithNullPassword() {
        return """
                {"name":"Maria Silva","email":"%s","birthDate":"1990-08-20","phone":"61990000000","password":null}
                """.formatted(USER_EMAIL);
    }
}
