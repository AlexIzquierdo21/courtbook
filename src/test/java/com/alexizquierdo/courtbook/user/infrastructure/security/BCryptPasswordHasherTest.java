package com.alexizquierdo.courtbook.user.infrastructure.security;

import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();
    private final PlainPassword password = new PlainPassword("Secret1!");

    @Test
    void matchesTheRightPasswordAndRejectsAnother() {
        PasswordHash hash = hasher.hash(password);

        assertThat(hash.value()).isNotEqualTo(password.value()).startsWith("$2");
        assertThat(hasher.matches(password, hash)).isTrue();
        assertThat(hasher.matches(new PlainPassword("Other1!x"), hash)).isFalse();
    }
}
