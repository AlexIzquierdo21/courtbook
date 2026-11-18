package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    private static final String RAW = "3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c";

    @Test
    void generateCreatesDistinctIds() {
        assertThat(UserId.generate()).isNotEqualTo(UserId.generate());
    }

    @Test
    void ofUuidKeepsValue() {
        UUID uuid = UUID.fromString(RAW);

        assertThat(UserId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void ofStringParsesUuid() {
        assertThat(UserId.of(RAW)).isEqualTo(UserId.of(UUID.fromString(RAW)));
    }

    @Test
    void toStringIsTheUuid() {
        assertThat(UserId.of(RAW).toString()).isEqualTo(RAW);
    }

    @Test
    void rejectsNullUuid() {
        assertThatThrownBy(() -> UserId.of((UUID) null))
                .isInstanceOf(UserDomainException.class);
    }

    @Test
    void canonicalConstructorAlsoRejectsNull() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(UserDomainException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-a-uuid", "123"})
    void rejectsInvalidString(String raw) {
        assertThatThrownBy(() -> UserId.of(raw))
                .isInstanceOf(UserDomainException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_USER");
    }
}
