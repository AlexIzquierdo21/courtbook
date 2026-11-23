package com.alexizquierdo.courtbook.user.application;

import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> users = new HashMap<>();

    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean existsAdmin() {
        return users.values().stream().anyMatch(User::isAdmin);
    }

    public int count() {
        return users.size();
    }
}
