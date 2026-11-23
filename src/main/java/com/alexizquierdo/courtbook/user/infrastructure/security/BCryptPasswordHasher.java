package com.alexizquierdo.courtbook.user.infrastructure.security;

import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Override
    public PasswordHash hash(PlainPassword plainPassword) {
        return new PasswordHash(encoder.encode(plainPassword.value()));
    }
    @Override
    public boolean matches(PlainPassword plainPassword, PasswordHash passwordHash) {
        return encoder.matches(plainPassword.value(), passwordHash.value());
    }
}
