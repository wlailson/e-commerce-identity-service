package io.wlailson.github.e_commerce_identity_service.repository;

import io.wlailson.github.e_commerce_identity_service.domain.User;
import io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Query("""
            SELECT new io.wlailson.github.e_commerce_identity_service.dto.UserResponseMinDTO(obj.id, obj.name, obj.email)
            FROM  User obj
            """)
    Page<UserResponseMinDTO> searchAllUsers(Pageable pageable);
}
