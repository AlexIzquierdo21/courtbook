package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.model.Role;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Email EMAIL = Email.of("ana@x.com");
    private static final PasswordHash HASH = new PasswordHash("$2a$10$hash");

    @Test
    void createAlwaysRegistersAPlayerWithTrimmedNames() {
        User user = User.create("  Ana ", " López ", EMAIL, HASH);

        assertThat(user.getRole()).isEqualTo(Role.PLAYER);
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("López");
        assertThat(user.getId()).isNotNull();
    }

    @Test
    void createAdminRegistersAnAdmin() {
        User admin = User.createAdmin("Ana", "López", EMAIL, HASH);

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.getId()).isNotNull();
    }

    @Test
    void createdPlayerIsNotAdmin() {
        assertThat(User.create("Ana", "López", EMAIL, HASH).isAdmin()).isFalse();
    }

    @Test
    void createAdminAlsoEnforcesInvariants() {
        assertThatThrownBy(() -> User.createAdmin(" ", "López", EMAIL, HASH))
                .isInstanceOf(UserDomainException.class);
        assertThatThrownBy(() -> User.createAdmin("Ana", "López", EMAIL, null))
                .isInstanceOf(UserDomainException.class);
    }

    @Test
    void restoreKeepsEveryField() {
        UserId id = UserId.generate();

        User user = User.restore(id, "Ana", "López", EMAIL, HASH, Role.ADMIN);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("López");
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getPasswordHash()).isEqualTo(HASH);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    // Each case breaks exactly one field, so the message proves which check fired.
    static Stream<Arguments> invalidUsers() {
        UserId id = UserId.generate();
        return Stream.of(
                Arguments.of("Id", (ThrowingCallable) () -> User.restore(null, "Ana", "López", EMAIL, HASH, Role.PLAYER)),
                Arguments.of("First name", (ThrowingCallable) () -> User.create(null, "López", EMAIL, HASH)),
                Arguments.of("First name", (ThrowingCallable) () -> User.create(" ", "López", EMAIL, HASH)),
                Arguments.of("Last name", (ThrowingCallable) () -> User.create("Ana", null, EMAIL, HASH)),
                Arguments.of("Last name", (ThrowingCallable) () -> User.create("Ana", " ", EMAIL, HASH)),
                Arguments.of("Email", (ThrowingCallable) () -> User.create("Ana", "López", null, HASH)),
                Arguments.of("Password hash", (ThrowingCallable) () -> User.create("Ana", "López", EMAIL, null)),
                Arguments.of("Role", (ThrowingCallable) () -> User.restore(id, "Ana", "López", EMAIL, HASH, null))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUsers")
    void rejectsMissingField(String field, ThrowingCallable build) {
        assertThatThrownBy(build)
                .isInstanceOf(UserDomainException.class)
                .hasMessageStartingWith(field + " cannot be")
                .hasFieldOrPropertyWithValue("code", "INVALID_USER");
    }

    @Test
    void identityIsTheId() {
        UserId id = UserId.generate();

        User user = User.restore(id, "Ana", "López", EMAIL, HASH, Role.PLAYER);
        User sameIdOtherData = User.restore(id, "Otra", "Persona", Email.of("otra@x.com"), HASH, Role.ADMIN);

        assertThat(user)
                .isEqualTo(sameIdOtherData)
                .hasSameHashCodeAs(sameIdOtherData)
                .isNotEqualTo(User.create("Ana", "López", EMAIL, HASH));
    }
}
