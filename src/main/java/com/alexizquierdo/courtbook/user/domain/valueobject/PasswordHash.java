package com.alexizquierdo.courtbook.user.domain.valueobject;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new UserDomainException("Password hash cannot be blank");
        }
    }

    // Keep the hash out of logs.
    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}
