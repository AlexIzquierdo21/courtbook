package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    private static final String RAW = "$2a$10$N9qo8uLOickgx2ZMRZoMye";

    @Test
    void keepsValue() {
        assertThat(new PasswordHash(RAW).value()).isEqualTo(RAW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsNullOrBlank(String raw) {
        assertThatThrownBy(() -> new PasswordHash(raw))
                .isInstanceOf(UserDomainException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_USER");
    }

    @Test
    void toStringNeverExposesTheHash() {
        assertThat(new PasswordHash(RAW).toString()).doesNotContain(RAW);
    }
}
