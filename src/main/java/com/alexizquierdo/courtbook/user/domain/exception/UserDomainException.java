package com.alexizquierdo.courtbook.user.domain.exception;

import com.alexizquierdo.courtbook.shared.domain.DomainException;

public class UserDomainException extends DomainException {

    public UserDomainException(String message) {
        super("INVALID_USER", message);
    }
}
