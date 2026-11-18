package com.alexizquierdo.courtbook.user.domain.model;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

import java.util.Objects;

public class User {

    private final UserId id;
    private final String firstName;
    private final String lastName;
    private final Email email;
    private final PasswordHash passwordHash;
    private final Role role;

    private User(UserId id, String firstName, String lastName,
                 Email email, PasswordHash passwordHash, Role role) {
        this.id = required(id, "Id");
        this.firstName = requiredText(firstName, "First name");
        this.lastName = requiredText(lastName, "Last name");
        this.email = required(email, "Email");
        this.passwordHash = required(passwordHash, "Password hash");
        this.role = required(role, "Role");
    }

    // Self-registration is always PLAYER; roles are never taken from the request.
    public static User create(String firstName, String lastName,
                              Email email, PasswordHash passwordHash) {
        return new User(UserId.generate(), firstName, lastName, email, passwordHash, Role.PLAYER);
    }

    // Only for the first-admin bootstrap and invitation acceptance
    public static User createAdmin(String firstName, String lastName,
                                   Email email, PasswordHash passwordHash) {
        return new User(UserId.generate(), firstName, lastName, email, passwordHash, Role.ADMIN);
    }

    public static User restore(UserId id, String firstName, String lastName,
                               Email email, PasswordHash passwordHash, Role role) {
        return new User(id, firstName, lastName, email, passwordHash, role);
    }

    public UserId getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Email getEmail() {
        return email;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new UserDomainException(field + " cannot be null");
        }
        return value;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new UserDomainException(field + " cannot be blank");
        }
        return value.trim();
    }
}
