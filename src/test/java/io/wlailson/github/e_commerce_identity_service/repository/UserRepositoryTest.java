package io.wlailson.github.e_commerce_identity_service.repository;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.0");

    @Autowired
    private UserRepository repository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByAuthority("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_USER must be created by the Flyway migration"));

        repository.saveAll(List.of(
                createUser("John Doe", "john@gmail.com", LocalDate.of(1970, 1, 1), role),
                createUser("Jane Doe", "jane@gmail.com", LocalDate.of(1980, 2, 2), role),
                createUser("Mary Doe", "mary@gmail.com", LocalDate.of(1990, 3, 3), role)
        ));
    }

    @Nested
    class SearchAllUsersTests {

        @Test
        void shouldReturnAllUsersPageable() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserResponseMinDTO> result = repository.searchAllUsers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .extracting(UserResponseMinDTO::email)
                    .containsExactlyInAnyOrder("john@gmail.com", "jane@gmail.com", "mary@gmail.com");
        }

        @Test
        void shouldReturnOnlyProjectedUserData() {
            Page<UserResponseMinDTO> result = repository.searchAllUsers(PageRequest.of(0, 10));

            UserResponseMinDTO user = result.getContent().stream()
                    .filter(item -> item.email().equals("john@gmail.com"))
                    .findFirst()
                    .orElseThrow();

            assertThat(user.id()).isNotNull();
            assertThat(user.name()).isEqualTo("John Doe");
            assertThat(user.email()).isEqualTo("john@gmail.com");
        }

        @Test
        void shouldPaginateUsers() {
            Page<UserResponseMinDTO> firstPage = repository.searchAllUsers(PageRequest.of(0, 2));
            Page<UserResponseMinDTO> secondPage = repository.searchAllUsers(PageRequest.of(1, 2));

            assertThat(firstPage.getTotalElements()).isEqualTo(3);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(firstPage.getSize()).isEqualTo(2);
            assertThat(firstPage.getNumberOfElements()).isEqualTo(2);
            assertThat(secondPage.getNumberOfElements()).isEqualTo(1);

            Set<String> emails = new HashSet<>(firstPage.getContent().stream()
                    .map(UserResponseMinDTO::email)
                    .toList());
            emails.addAll(secondPage.getContent().stream()
                    .map(UserResponseMinDTO::email)
                    .toList());

            assertThat(emails)
                    .containsExactlyInAnyOrder("john@gmail.com", "jane@gmail.com", "mary@gmail.com");
        }

        @Test
        void shouldReturnEmptyPageWhenThereAreNoUsers() {
            repository.deleteAll();

            Page<UserResponseMinDTO> result = repository.searchAllUsers(PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }


    private User createUser(String name, String email, LocalDate birthDate, Role role) {
        return new User(
                null,
                name,
                email,
                "61996695660",
                birthDate,
                "123456",
                Set.of(role)
        );
    }
}