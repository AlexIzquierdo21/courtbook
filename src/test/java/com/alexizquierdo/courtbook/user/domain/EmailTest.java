package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;
import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesBeforeValidating() {
        assertThat(Email.of("  Ana@X.COM ").value()).isEqualTo("ana@x.com");
    }

    @Test
    void acceptsLongTlds() {
        assertThat(Email.of("ana@studio.photography").value()).isEqualTo("ana@studio.photography");
    }

    @Test
    void equalByValue() {
        assertThat(Email.of("ANA@x.com")).isEqualTo(Email.of("ana@x.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ana", "ana@x", "ana@x.c", "a b@x.com"})
    void rejectsInvalidWithoutLeakingValue(String raw) {
        assertThatThrownBy(() -> Email.of(raw))
                .isInstanceOf(InvalidEmailException.class)
                .isInstanceOf(UserDomainException.class)
                .hasMessage("Email format is not valid")
                .hasFieldOrPropertyWithValue("code", "INVALID_EMAIL");
    }

    @Test
    void toStringIsTheNormalizedValue() {
        assertThat(Email.of(" Ana@X.com ")).hasToString("ana@x.com");
    }
}
