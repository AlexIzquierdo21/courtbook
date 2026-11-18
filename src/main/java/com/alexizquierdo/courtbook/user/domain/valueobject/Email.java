package com.alexizquierdo.courtbook.user.domain.valueobject;

import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public Email {
        if (value == null) {
            throw new InvalidEmailException();
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException();
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
