package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.user.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

}
