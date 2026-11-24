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

import static org.assertj.core.api.Assertions.assertThat;

// Registered before SpringExtension so the startup logs (where the runner fires) are captured too.
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "courtbook.bootstrap.admin.email=" + AdminBootstrapIntegrationTest.EMAIL,
        "courtbook.bootstrap.admin.password=" + AdminBootstrapIntegrationTest.PASSWORD})
@Import(TestcontainersConfiguration.class)
class AdminBootstrapIntegrationTest {

    static final String EMAIL = "root@courtbook.dev";
    static final String PASSWORD = "Bootstr4p!";

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
