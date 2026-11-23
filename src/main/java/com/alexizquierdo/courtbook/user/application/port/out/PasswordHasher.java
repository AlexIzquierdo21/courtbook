package com.alexizquierdo.courtbook.user.application.port.out;

import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;

public interface PasswordHasher {

    PasswordHash hash(PlainPassword plainPassword);

    boolean matches(PlainPassword plainPassword, PasswordHash passwordHash);
}
