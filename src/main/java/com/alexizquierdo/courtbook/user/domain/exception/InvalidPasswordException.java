package com.alexizquierdo.courtbook.user.domain.exception;


public class InvalidPasswordException extends UserDomainException {
    public InvalidPasswordException(String message) {
        super("INVALID_PASSWORD", message);
    }
}
