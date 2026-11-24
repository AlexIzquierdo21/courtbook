package com.alexizquierdo.courtbook.user.application.port.in;

import com.alexizquierdo.courtbook.user.domain.model.User;

import java.util.Optional;

public interface BootstrapAdminUseCase {

    // Empty when an ADMIN already exists.
    Optional<User> bootstrap(BootstrapAdminCommand command);
}
