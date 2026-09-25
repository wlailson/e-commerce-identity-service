package io.wlailson.github.e_commerce_identity_service.controller;

import io.wlailson.github.e_commerce_identity_service.dto.UserRequestDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import io.wlailson.github.e_commerce_identity_service.handlers.GlobalExceptionHandler;
import io.wlailson.github.e_commerce_identity_service.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, UserControllerTest.SecurityTestConfig.class})
class UserControllerTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "maria@email.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @Nested
    class GetUserById {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturnUserForAdmin() throws Exception {
            when(service.getUserById(USER_ID)).thenReturn(userResponse());

            mockMvc.perform(get("/users/{userId}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID))
                    .andExpect(jsonPath("$.name").value("Maria"))
                    .andExpect(jsonPath("$.email").value(USER_EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

            verify(service).getUserById(USER_ID);
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldRejectRegularUser() throws Exception {
            mockMvc.perform(get("/users/{userId}", USER_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}", USER_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturnPagedUsersForAdmin() throws Exception {
            when(service.getAllUsers(any())).thenReturn(
                    new PageImpl<>(
                            List.of(new UserResponseMinDTO(USER_ID, "Maria", USER_EMAIL)),
                            PageRequest.of(1, 5),
                            6
                    )
            );

            mockMvc.perform(get("/users?page=1&size=5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(USER_ID))
                    .andExpect(jsonPath("$.content[0].email").value(USER_EMAIL))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(5));

            ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                    ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
            verify(service).getAllUsers(pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldRejectRegularUser() throws Exception {
            mockMvc.perform(get("/users"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class GetCurrentUser {

        @Test
        @WithMockUser(username = USER_EMAIL, roles = "USER")
        void shouldReturnAuthenticatedUser() throws Exception {
            when(service.getUserByEmail(USER_EMAIL)).thenReturn(userResponse());

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID))
                    .andExpect(jsonPath("$.email").value(USER_EMAIL));

            verify(service).getUserByEmail(USER_EMAIL);
        }

        @Test
        @WithMockUser(username = "admin@email.com", roles = "ADMIN")
        void shouldReturnAuthenticatedAdmin() throws Exception {
            when(service.getUserByEmail("admin@email.com")).thenReturn(userResponse());

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk());

            verify(service).getUserByEmail("admin@email.com");
        }

        @Test
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PostUser {

        @Test
        void shouldCreateUserWithoutAuthentication() throws Exception {
            when(service.postUser(any(UserRequestDTO.class))).thenReturn(userResponse());

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/users/" + USER_ID))
                    .andExpect(jsonPath("$.email").value(USER_EMAIL));

            verify(service).postUser(any(UserRequestDTO.class));
        }

        @Test
        void shouldRejectInvalidRequest() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJsonWithBlankPassword()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos"))
                    .andExpect(jsonPath("$.errors.password").value("Password is required"));
        }
    }

    @Nested
    class Login {

        @Test
        void shouldReturnToken() throws Exception {
            when(service.login(any(UserRequestDTO.class))).thenReturn("jwt-token");

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("jwt-token"));

            verify(service).login(any(UserRequestDTO.class));
        }
    }

    @Nested
    class PutUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateUserForAuthenticatedUser() throws Exception {
            when(service.putUser(any(Long.class), any(UserRequestDTO.class), any()))
                    .thenReturn(userResponse());

            mockMvc.perform(put("/users/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID))
                    .andExpect(jsonPath("$.name").value("Maria"));

            verify(service).putUser(any(Long.class), any(UserRequestDTO.class), any());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldAllowRegularUser() throws Exception {
            when(service.putUser(any(Long.class), any(UserRequestDTO.class), any()))
                    .thenReturn(userResponse());

            mockMvc.perform(put("/users/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(put("/users/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUserJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldRejectInvalidRequest() throws Exception {
            mockMvc.perform(put("/users/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJsonWithBlankPassword()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").value("Password is required"));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteUserForAdmin() throws Exception {
            doNothing().when(service).deleteUser(eq(USER_ID), any());

            mockMvc.perform(delete("/users/{userId}", USER_ID))
                    .andExpect(status().isNoContent());

            verify(service).deleteUser(eq(USER_ID), any());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldRejectRegularUser() throws Exception {
            mockMvc.perform(delete("/users/{userId}", USER_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(delete("/users/{userId}", USER_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    private UserResponseDTO userResponse() {
        return new UserResponseDTO(
                USER_ID,
                "Maria",
                USER_EMAIL,
                "61996695660",
                LocalDate.of(1990, 8, 20),
                List.of("ROLE_USER")
        );
    }

    private String validUserJson() {
        return """
                {"name":"Maria","email":"%s","birthDate":"1990-08-20","phone":"61996695660","password":"123456"}
                """.formatted(USER_EMAIL);
    }

    private String userJsonWithBlankPassword() {
        return """
                {"name":"Maria","email":"%s","birthDate":"1990-08-20","phone":"61996695660","password":" "}
                """.formatted(USER_EMAIL);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityTestConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/users/login").permitAll()
                            .requestMatchers(HttpMethod.POST, "/users").permitAll()
                            .anyRequest().authenticated())
                    .build();
        }
    }
}
