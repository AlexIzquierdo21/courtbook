package com.alexizquierdo.courtbook.user.application;

import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public PasswordHash hash(PlainPassword plainPassword) {
        return new PasswordHash("hashed:" + plainPassword.value());
    }

    @Override
    public boolean matches(PlainPassword plainPassword, PasswordHash passwordHash) {
        return hash(plainPassword).equals(passwordHash);
    }
}
