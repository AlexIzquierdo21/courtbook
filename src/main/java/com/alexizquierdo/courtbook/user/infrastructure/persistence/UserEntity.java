package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.user.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserEntity {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // null means "new" for Spring Data, so save() does persist instead of merge.
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserEntity() {
    }

    UserEntity(UUID id, String firstName, String lastName, String email, String passwordHash, Role role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    UUID getId() { return id; }
    String getFirstName() { return firstName; }
    String getLastName() { return lastName; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    Role getRole() { return role; }
}
