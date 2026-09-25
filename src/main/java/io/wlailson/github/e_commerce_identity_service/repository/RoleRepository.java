package io.wlailson.github.e_commerce_identity_service.repository;

import io.wlailson.github.e_commerce_identity_service.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByAuthority(String authority);
}
