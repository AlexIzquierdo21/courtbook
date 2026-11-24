package com.alexizquierdo.courtbook.user.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Set through environment variables (COURTBOOK_BOOTSTRAP_ADMIN_EMAIL / _PASSWORD), never with values in application.yaml.
@ConfigurationProperties("courtbook.bootstrap.admin")
public record BootstrapAdminProperties(String email, String password) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    // Records print every field by default; keep the password out of logs.
    @Override
    public String toString() {
        return "BootstrapAdminProperties[email=" + email + ", password=***]";
    }
}
