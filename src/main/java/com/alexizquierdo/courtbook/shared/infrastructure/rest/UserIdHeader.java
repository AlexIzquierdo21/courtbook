package com.alexizquierdo.courtbook.shared.infrastructure.rest;

// Phase 1 identity (D10), insecure on purpose until there is a login.
// Controllers take it as @RequestHeader(UserIdHeader.NAME) UUID requesterId: Spring parses the UUID,
// and a missing or malformed header ends up as a 400 ProblemDetail in GlobalExceptionHandler.
public final class UserIdHeader {

    public static final String NAME = "X-User-Id";

    private UserIdHeader() {
    }
}
