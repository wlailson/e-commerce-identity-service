package io.wlailson.github.e_commerce_identity_service.security;

import io.wlailson.github.e_commerce_identity_service.controller.UserController;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseDTO;
import io.wlailson.github.e_commerce_identity_service.handlers.GlobalExceptionHandler;
import io.wlailson.github.e_commerce_identity_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
        properties = "cors.origins=http://localhost"
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class IdentityBearerAuthenticationTest {

    private static final String TOKEN = "test-access-token";
    private static final String USER_EMAIL = "user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void acceptsBearerTokenWithUserRoleOnCurrentUserEndpoint() throws Exception {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtWithRole("ROLE_USER"));
        when(userService.getUserByEmail(USER_EMAIL)).thenReturn(userResponse());

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk());

        verify(userService).getUserByEmail(USER_EMAIL);
    }

    @Test
    void enforcesAdminRoleFromBearerToken() throws Exception {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtWithRole("ROLE_USER"));

        mockMvc.perform(get("/users/{userId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }

    private static Jwt jwtWithRole(String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject(USER_EMAIL)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("roles", List.of(role))
                .build();
    }

    private static UserResponseDTO userResponse() {
        return new UserResponseDTO(
                1L,
                "Test User",
                USER_EMAIL,
                "123456789",
                LocalDate.of(1990, 1, 1),
                List.of("ROLE_USER")
        );
    }
}
