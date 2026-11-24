package com.alexizquierdo.courtbook.user.application.usecase;

import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminCommand;
import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminUseCase;
import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;

import java.util.Optional;

public class BootstrapAdminService implements BootstrapAdminUseCase {

    // D16 only configures email and password; the names are placeholders until users can edit their profile.
    static final String FIRST_NAME = "Admin";
    static final String LAST_NAME = "CourtBook";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public BootstrapAdminService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> bootstrap(BootstrapAdminCommand command) {
        // Checked before validating: once an ADMIN exists the properties are dead config and must not break startup.
        if (userRepository.existsAdmin()) {
            return Optional.empty();
        }

        Email email = Email.of(command.email());
        PlainPassword password = new PlainPassword(command.plainPassword());

        // Promoting an existing PLAYER is out of scope (R9).
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User admin = User.createAdmin(FIRST_NAME, LAST_NAME, email, passwordHasher.hash(password));
        return Optional.of(userRepository.save(admin));
    }
}
