package com.alexizquierdo.courtbook.user.domain.exception;


public class InvalidEmailException extends UserDomainException {

    public InvalidEmailException() {
        super("INVALID_EMAIL", "Email format is not valid");
    }
}
