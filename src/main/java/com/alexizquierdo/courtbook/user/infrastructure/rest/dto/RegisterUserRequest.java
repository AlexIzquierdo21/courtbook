package com.alexizquierdo.courtbook.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No role field on purpose: self-registration is always PLAYER, a "role" in the JSON is ignored.
public record RegisterUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        // Format is checked by the Email value object (INVALID_EMAIL); the size matches the VARCHAR(255) column.
        @NotBlank @Size(max = 255) String email,
        // Rules live in PlainPassword (INVALID_PASSWORD).
        @NotBlank String password) {

    @Override
    public String toString() {
        return "RegisterUserRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}

