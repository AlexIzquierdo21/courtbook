package com.alexizquierdo.courtbook.user.domain.exception;

import com.alexizquierdo.courtbook.shared.domain.DomainException;

public class InvalidPasswordException extends DomainException {
    public InvalidPasswordException(String message) {
        super("INVALID_PASSWORD", message);
    }
}
