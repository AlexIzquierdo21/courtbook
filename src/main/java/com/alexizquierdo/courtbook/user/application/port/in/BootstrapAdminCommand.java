package com.alexizquierdo.courtbook.user.application.port.in;

public record BootstrapAdminCommand(String email, String plainPassword) {

    // Records print every field by default; keep the password out of logs.
    @Override
    public String toString() {
        return "BootstrapAdminCommand[email=" + email + ", plainPassword=***]";
    }
}
