package com.alexizquierdo.courtbook.user.application;

import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminCommand;
import com.alexizquierdo.courtbook.user.application.usecase.BootstrapAdminService;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidPasswordException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapAdminServiceTest {

    private static final String EMAIL = "root@courtbook.dev";
    private static final String PASSWORD = "Bootstr4p!";
    private static final PasswordHash HASH = new PasswordHash("hashed:Secret1!");

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final BootstrapAdminService service = new BootstrapAdminService(repository, hasher);

    private static BootstrapAdminCommand command(String email) {
        return new BootstrapAdminCommand(email, PASSWORD);
    }

    @Test
    void createsAnAdminWithTheHashedPasswordWhenThereIsNone() {
        User admin = service.bootstrap(command(EMAIL)).orElseThrow();

        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.getEmail()).isEqualTo(Email.of(EMAIL));
        assertThat(hasher.matches(new PlainPassword(PASSWORD), admin.getPasswordHash())).isTrue();
        assertThat(admin.getPasswordHash().value()).isNotEqualTo(PASSWORD);
        assertThat(repository.findById(admin.getId())).contains(admin);
    }

    @Test
    void createsNothingWhenAnAdminAlreadyExistsEvenWithAnotherEmail() {
        repository.save(User.createAdmin("Ana", "López", Email.of("ana@x.com"), HASH));

        assertThat(service.bootstrap(command(EMAIL))).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsByEmail(Email.of(EMAIL))).isFalse();
    }

    @Test
    void isIdempotent() {
        service.bootstrap(command(EMAIL));

        assertThat(service.bootstrap(command(EMAIL))).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
    }

    // Left-over invalid properties must not break the startup once the admin exists.
    @Test
    void ignoresTheCommandWhenAnAdminAlreadyExists() {
        repository.save(User.createAdmin("Ana", "López", Email.of("ana@x.com"), HASH));

        assertThat(service.bootstrap(new BootstrapAdminCommand("not-an-email", "short"))).isEmpty();
    }

    @Test
    void rejectsInvalidPropertiesWithoutSaving() {
        assertThatThrownBy(() -> service.bootstrap(command("not-an-email")))
                .isInstanceOf(InvalidEmailException.class);
        assertThatThrownBy(() -> service.bootstrap(new BootstrapAdminCommand(EMAIL, "short")))
                .isInstanceOf(InvalidPasswordException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsAnEmailAlreadyRegisteredAsPlayer() {
        repository.save(User.create("Ana", "López", Email.of(EMAIL), HASH));

        assertThatThrownBy(() -> service.bootstrap(command(EMAIL.toUpperCase())))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(repository.existsAdmin()).isFalse();
    }

    @Test
    void commandToStringHidesThePassword() {
        assertThat(command(EMAIL).toString()).doesNotContain(PASSWORD);
    }
}
