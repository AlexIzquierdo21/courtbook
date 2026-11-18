package com.alexizquierdo.courtbook.user.domain;

import com.alexizquierdo.courtbook.user.domain.exception.UserDomainException;
import com.alexizquierdo.courtbook.user.domain.model.Role;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

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
    void restoreKeepsIdAndRole() {
        UserId id = UserId.generate();

        User user = User.restore(id, "Ana", "López", EMAIL, HASH, Role.ADMIN);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void restoreAlsoEnforcesInvariants() {
        assertThatThrownBy(() -> User.restore(UserId.generate(), " ", "López", EMAIL, HASH, Role.PLAYER))
                .isInstanceOf(UserDomainException.class);
        assertThatThrownBy(() -> User.restore(UserId.generate(), "Ana", "López", EMAIL, HASH, null))
                .isInstanceOf(UserDomainException.class);
    }

    @Test
    void identityIsTheId() {
        UserId id = UserId.generate();

        assertThat(User.restore(id, "Ana", "López", EMAIL, HASH, Role.PLAYER))
                .isEqualTo(User.restore(id, "Otra", "Persona", Email.of("otra@x.com"), HASH, Role.ADMIN))
                .isNotEqualTo(User.create("Ana", "López", EMAIL, HASH));
    }

    @Test
    void passwordHashIsNotPrinted() {
        assertThat(HASH.toString()).doesNotContain("hash");
    }
}
