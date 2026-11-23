package com.alexizquierdo.courtbook.user.application.port.in;

import com.alexizquierdo.courtbook.user.domain.model.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}
