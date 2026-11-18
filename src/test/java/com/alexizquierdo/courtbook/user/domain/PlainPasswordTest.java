package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.InvalidPasswordException;
import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainPasswordTest {

    // Smallest prefix that satisfies every composition rule (4 bytes).
    private static final String RULES = "Aa1!";

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef1!", "Ñandú12!", "P@ssw0rd_€"})
    void acceptsValidPasswords(String raw) {
        assertThat(new PlainPassword(raw).value()).isEqualTo(raw);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new PlainPassword(null))
                .isInstanceOf(InvalidPasswordException.class)
                .isInstanceOf(UserDomainException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PASSWORD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abc def1!", "Abcdef1!\t", " Abcdef1!", "Abcdef1!\n", "Abc def1!"})
    void rejectsWhitespace(String raw) {
        assertThatThrownBy(() -> new PlainPassword(raw))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Plain password must not contain whitespace");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Abcde1!"})
    void rejectsShorterThanEight(String raw) {
        assertThatThrownBy(() -> new PlainPassword(raw))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void acceptsExactly72Bytes() {
        assertThat(new PlainPassword(RULES + "a".repeat(68))).isNotNull();
        assertThat(new PlainPassword(RULES + "ñ".repeat(34))).isNotNull();
    }

    @Test
    void rejectsOver72AsciiBytes() {
        assertThatThrownBy(() -> new PlainPassword(RULES + "a".repeat(69)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("72 bytes");
    }

    @Test
    void countsBytesNotCharacters() {
        String raw = RULES + "ñ".repeat(35); // 39 chars, 74 bytes

        assertThat(raw.length()).isLessThan(72);
        assertThatThrownBy(() -> new PlainPassword(raw))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("72 bytes");
    }

    @ParameterizedTest
    @CsvSource({
            "abcdef1!, uppercase",
            "ABCDEF1!, lowercase",
            "Abcdefg!, digit",
            "Abcdefg1, special"
    })
    void rejectsMissingCompositionRule(String raw, String rule) {
        assertThatThrownBy(() -> new PlainPassword(raw))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining(rule);
    }

    @Test
    void neverExposesTheValue() {
        String secret = "Abcdef1!";

        assertThat(new PlainPassword(secret).toString()).doesNotContain(secret);
        assertThatThrownBy(() -> new PlainPassword("abcdef1!"))
                .hasMessageNotContaining("abcdef1!");
    }
}
