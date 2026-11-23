package com.alexizquierdo.courtbook.user.application;

import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserCommand;
import com.alexizquierdo.courtbook.user.application.usecase.RegisterUserService;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidPasswordException;
import com.alexizquierdo.courtbook.user.domain.model.Role;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserServiceTest {

    private static final String PASSWORD = "Secret1!";

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final RegisterUserService service = new RegisterUserService(repository, hasher);

    private static RegisterUserCommand command(String email) {
        return new RegisterUserCommand("Ana", "López", email, PASSWORD);
    }

    @Test
    void registersAPlayerAndStoresTheHashNotThePlainPassword() {
        User user = service.register(command("ana@x.com"));

        assertThat(user.getRole()).isEqualTo(Role.PLAYER);
        assertThat(user.getPasswordHash().value()).isEqualTo("hashed:" + PASSWORD);
        assertThat(hasher.matches(new PlainPassword(PASSWORD), user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash().value()).isNotEqualTo(PASSWORD);
        assertThat(repository.findById(user.getId())).contains(user);
    }

    @Test
    void rejectsADuplicateEmailEvenWithDifferentCase() {
        service.register(command("ana@x.com"));

        assertThatThrownBy(() -> service.register(command("ANA@X.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidInputWithoutSaving() {
        assertThatThrownBy(() -> service.register(command("not-an-email")))
                .isInstanceOf(InvalidEmailException.class);
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("Ana", "López", "ana@x.com", "short")))
                .isInstanceOf(InvalidPasswordException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void commandToStringHidesThePassword() {
        assertThat(command("ana@x.com").toString()).doesNotContain(PASSWORD);
    }
}
