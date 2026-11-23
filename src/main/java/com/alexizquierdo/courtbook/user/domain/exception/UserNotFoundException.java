package com.alexizquierdo.courtbook.user.domain.exception;

import com.alexizquierdo.courtbook.shared.domain.DomainException;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(UserId id) {
        super("USER_NOT_FOUND", "User " + id + " not found");
    }
}
