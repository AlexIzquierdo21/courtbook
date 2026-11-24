package com.alexizquierdo.courtbook.user.infrastructure.bootstrap;

import com.alexizquierdo.courtbook.TestcontainersConfiguration;
import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Registered before SpringExtension so the startup logs (where the runner fires) are captured too.
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AdminBootstrapIntegrationTest {

    private static final String EMAIL = "root@courtbook.dev";
    // Generated, not hardcoded: no password literal for secret scanners (GitGuardian) to flag.
    // "Aa1!" satisfies PlainPassword's character rules; the UUID adds length.
    private static final String PASSWORD = "Aa1!" + UUID.randomUUID();

    @DynamicPropertySource
    static void adminProperties(DynamicPropertyRegistry registry) {
        registry.add("courtbook.bootstrap.admin.email", () -> EMAIL);
        registry.add("courtbook.bootstrap.admin.password", () -> PASSWORD);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private AdminBootstrapRunner runner;

    @Autowired
    private JdbcTemplate jdbc;

    private int adminCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE role = 'ADMIN'", Integer.class);
    }

    @Test
    void startupCreatesTheAdminOnceAndNeverLogsThePassword(CapturedOutput output) {
        User admin = userRepository.findByEmail(Email.of(EMAIL)).orElseThrow();
        assertThat(admin.isAdmin()).isTrue();
        assertThat(passwordHasher.matches(new PlainPassword(PASSWORD), admin.getPasswordHash())).isTrue();
        assertThat(adminCount()).isEqualTo(1);

        // Second startup against the same database.
        runner.run(new DefaultApplicationArguments());

        assertThat(adminCount()).isEqualTo(1);
        assertThat(output).contains("Bootstrap ADMIN created: " + EMAIL)
                .contains("an ADMIN already exists")
                .doesNotContain(PASSWORD);
    }
}
