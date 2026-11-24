package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.model.Role;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

import java.util.Optional;

public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    // shortcut: always inserts (the domain carries no version); when users can be edited, load the entity and update it.
    @Override
    public User save(User user) {
        return UserEntityMapper.toDomain(repository.save(UserEntityMapper.toEntity(user)));
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
