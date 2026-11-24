package com.alexizquierdo.courtbook.user.infrastructure.bootstrap;

import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminCommand;
import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

// Creates the first ADMIN at startup (D16, R10).
// Invalid properties or an email already taken by a PLAYER propagate and stop the startup: the config is wrong.
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapAdminUseCase bootstrapAdminUseCase;
    private final BootstrapAdminProperties properties;

    public AdminBootstrapRunner(BootstrapAdminUseCase bootstrapAdminUseCase, BootstrapAdminProperties properties) {
        this.bootstrapAdminUseCase = bootstrapAdminUseCase;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.warn("Admin bootstrap skipped: courtbook.bootstrap.admin.email and .password are not both set");
            return;
        }
        bootstrapAdminUseCase.bootstrap(new BootstrapAdminCommand(properties.email(), properties.password()))
                .ifPresentOrElse(
                        admin -> log.info("Bootstrap ADMIN created: {}", admin.getEmail()),
                        () -> log.info("Admin bootstrap skipped: an ADMIN already exists"));
    }
}
