package com.alexizquierdo.courtbook.user.domain.valueobject;

import java.util.UUID;

public record UserId(UUID value) {

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("UserId value cannot be null");
        }
        return new UserId(value);
    }

    public static UserId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId value cannot be null or blank");
        }
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
