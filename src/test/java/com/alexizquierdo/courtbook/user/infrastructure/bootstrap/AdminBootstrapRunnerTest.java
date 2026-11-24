package com.alexizquierdo.courtbook.user.infrastructure.bootstrap;

import com.alexizquierdo.courtbook.user.application.FakePasswordHasher;
import com.alexizquierdo.courtbook.user.application.InMemoryUserRepository;
import com.alexizquierdo.courtbook.user.application.usecase.BootstrapAdminService;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class AdminBootstrapRunnerTest {

    private static final String EMAIL = "root@courtbook.dev";
    // Generated, not hardcoded: no password literal for secret scanners (GitGuardian) to flag.
    // "Aa1!" satisfies PlainPassword's character rules; the UUID adds length.
    private static final String PASSWORD = "Aa1!" + UUID.randomUUID();
    private static final ApplicationArguments NO_ARGS = new DefaultApplicationArguments();

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final BootstrapAdminService useCase = new BootstrapAdminService(repository, new FakePasswordHasher());

    private AdminBootstrapRunner runner(String email, String password) {
        return new AdminBootstrapRunner(useCase, new BootstrapAdminProperties(email, password));
    }

    @Test
    void createsTheAdminWithoutLoggingThePassword(CapturedOutput output) {
        runner(EMAIL, PASSWORD).run(NO_ARGS);

        assertThat(repository.existsAdmin()).isTrue();
        assertThat(output).contains("Bootstrap ADMIN created: " + EMAIL).doesNotContain(PASSWORD);
    }

    @Test
    void runningTwiceKeepsASingleAdmin(CapturedOutput output) {
        AdminBootstrapRunner runner = runner(EMAIL, PASSWORD);

        runner.run(NO_ARGS);
        runner.run(NO_ARGS);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(output).contains("an ADMIN already exists").doesNotContain(PASSWORD);
    }

    private static Stream<Arguments> incompleteProperties() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(EMAIL, null),
                Arguments.of(null, PASSWORD),
                Arguments.of(" ", " "));
    }

    // Missing, partial or blank properties: nothing is created and a WARN says why.
    @ParameterizedTest
    @MethodSource("incompleteProperties")
    void withoutPropertiesDoesNothingAndWarns(String email, String password, CapturedOutput output) {
        runner(email, password).run(NO_ARGS);

        assertThat(repository.count()).isZero();
        assertThat(output.getOut().lines())
                .anyMatch(line -> line.contains("WARN") && line.contains("Admin bootstrap skipped"));
        assertThat(output).doesNotContain(PASSWORD);
    }

    // Wrong config must stop the startup, not be logged and swallowed.
    @Test
    void invalidPropertiesOrAnEmailTakenByAPlayerStopTheStartup(CapturedOutput output) {
        assertThatThrownBy(() -> runner("not-an-email", PASSWORD).run(NO_ARGS))
                .isInstanceOf(InvalidEmailException.class);

        repository.save(User.create("Ana", "López", Email.of(EMAIL), new PasswordHash("hashed:Secret1!")));
        assertThatThrownBy(() -> runner(EMAIL, PASSWORD).run(NO_ARGS))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(repository.existsAdmin()).isFalse();
        assertThat(output).doesNotContain(PASSWORD);
    }

    @Test
    void propertiesToStringHidesThePassword() {
        assertThat(new BootstrapAdminProperties(EMAIL, PASSWORD).toString()).doesNotContain(PASSWORD);
    }
}
