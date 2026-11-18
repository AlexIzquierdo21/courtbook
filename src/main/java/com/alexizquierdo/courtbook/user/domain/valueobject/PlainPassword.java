package com.alexizquierdo.courtbook.user.domain.valueobject;

import com.alexizquierdo.courtbook.user.domain.exception.InvalidPasswordException;

import java.nio.charset.StandardCharsets;

public record PlainPassword(String value) {

    public PlainPassword {
        if (value == null) {
            throw new InvalidPasswordException("Plain password cannot be null");
        }

        // isSpaceChar also catches the non-breaking space (U+00A0), which isWhitespace misses.
        if (value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new InvalidPasswordException("Plain password must not contain whitespace");
        }

        if (value.length() < 8) {
            throw new InvalidPasswordException("Plain password must be at least 8 characters long");
        }

        if (value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException("Plain password must not exceed 72 bytes in UTF-8 encoding");
        }

        if (value.codePoints().noneMatch(Character::isUpperCase)) {
            throw new InvalidPasswordException("Plain password must contain at least one uppercase letter");
        }

        if (value.codePoints().noneMatch(Character::isLowerCase)) {
            throw new InvalidPasswordException("Plain password must contain at least one lowercase letter");
        }

        if (value.codePoints().noneMatch(Character::isDigit)) {
            throw new InvalidPasswordException("Plain password must contain at least one digit");
        }

        if (value.codePoints().noneMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new InvalidPasswordException("Plain password must contain at least one special character");
        }
    }

    @Override
    public String toString() {
        return "PlainPassword[***]";
    }

}
