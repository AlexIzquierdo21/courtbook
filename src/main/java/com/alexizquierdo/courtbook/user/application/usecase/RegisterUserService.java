package com.alexizquierdo.courtbook.user.application.usecase;

import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserCommand;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PlainPassword;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        PlainPassword password = new PlainPassword(command.plainPassword());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.create(command.firstName(), command.lastName(), email, passwordHasher.hash(password));
        return userRepository.save(user);
    }
}
