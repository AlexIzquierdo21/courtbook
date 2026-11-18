package com.alexizquierdo.courtbook.user.domain.valueobject;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new UserDomainException("UserId value cannot be null");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        if (value == null || value.isBlank()) {
            throw new UserDomainException("UserId value cannot be null or blank");
        }
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new UserDomainException("UserId value is not a valid UUID");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
