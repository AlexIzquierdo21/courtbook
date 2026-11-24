package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.model.Role;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class UserPersistenceAdapter implements UserRepository {

    // Name of the constraint in V1__create_app_user.sql.
    private static final String EMAIL_UNIQUE_KEY = "uk_app_user_email";

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    // shortcut: always inserts (the domain carries no version); when users can be edited, load the entity and update it.
    // Flushes so that losing a registration race on the email unique key fails here, not at commit,
    // and becomes the same EMAIL_ALREADY_REGISTERED as the existsByEmail pre-check (doc 03, "doble validación").
    @Override
    public User save(User user) {
        try {
            return UserEntityMapper.toDomain(repository.saveAndFlush(UserEntityMapper.toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(EMAIL_UNIQUE_KEY)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw e;
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public boolean existsAdmin() {
        return repository.existsByRole(Role.ADMIN);
    }
}
