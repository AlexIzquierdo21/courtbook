package com.alexizquierdo.courtbook.user.domain.exception;

import com.alexizquierdo.courtbook.shared.domain.DomainException;

public class InvalidEmailException extends DomainException {

    public InvalidEmailException() {
        super("INVALID_EMAIL", "Email format is not valid");
    }
}
