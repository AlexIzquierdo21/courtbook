package com.alexizquierdo.courtbook.user.domain.exception;

import com.alexizquierdo.courtbook.shared.domain.DomainException;

// Not a UserDomainException: it is a conflict (409), not an invalid-input error.
public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super("EMAIL_ALREADY_REGISTERED", "Email is already registered");
    }
}
