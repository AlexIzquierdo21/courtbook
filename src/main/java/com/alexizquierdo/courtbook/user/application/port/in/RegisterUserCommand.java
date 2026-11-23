package com.alexizquierdo.courtbook.user.application.port.in;

public record RegisterUserCommand(String firstName, String lastName, String email, String plainPassword) {

    // Records print every field by default; keep the password out of logs.
    @Override
    public String toString() {
        return "RegisterUserCommand[firstName=" + firstName + ", lastName=" + lastName
                + ", email=" + email + ", plainPassword=***]";
    }
}
