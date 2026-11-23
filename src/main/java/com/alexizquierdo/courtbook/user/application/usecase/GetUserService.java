package com.alexizquierdo.courtbook.user.application.usecase;

import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.domain.exception.UserNotFoundException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

public class GetUserService implements GetUserQuery {

    private final UserRepository userRepository;

    public GetUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserById(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
