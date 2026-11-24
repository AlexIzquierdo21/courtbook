package com.alexizquierdo.courtbook.user.infrastructure.rest.mapper;

import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserCommand;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.infrastructure.rest.dto.RegisterUserRequest;
import com.alexizquierdo.courtbook.user.infrastructure.rest.dto.UserResponse;

public class UserRestMapper {

    private UserRestMapper() {
    }

    public static RegisterUserCommand toCommand(RegisterUserRequest request) {
        return new RegisterUserCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password()
        );
    }

    // The password hash never leaves the domain.
    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail().value(),
                user.getRole().name()
        );
    }
}
