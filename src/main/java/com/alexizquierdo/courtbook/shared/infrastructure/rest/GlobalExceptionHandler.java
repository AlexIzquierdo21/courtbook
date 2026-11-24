package com.alexizquierdo.courtbook.shared.infrastructure.rest;

import com.alexizquierdo.courtbook.shared.domain.DomainException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Maps errors to RFC 9457 ProblemDetail with a stable "code" (doc 04).
// The status comes from the code, not the exception type, so shared does not depend on any module.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://courtbook.dev/errors/";

    // Value-object validation. Other INVALID_* codes (INVALID_SLOT...) are business rules: 422.
    private static final Set<String> BAD_REQUEST_CODES = Set.of("INVALID_EMAIL", "INVALID_PASSWORD", "INVALID_USER");

    record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        return problem(statusFor(ex.code()), ex.code(), ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // Spring's own errors (malformed JSON, bad UUID in path or X-User-Id, missing header...) also get a code.
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            HttpStatus known = HttpStatus.resolve(status.value());
            problem.setProperty("code", known != null ? known.name() : "HTTP_" + status.value());
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private static HttpStatus statusFor(String code) {
        if (BAD_REQUEST_CODES.contains(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code.endsWith("_NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.contains("_ALREADY_")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        String words = code.toLowerCase(Locale.ROOT);
        problem.setType(URI.create(TYPE_BASE + words.replace('_', '-')));
        // EMAIL_ALREADY_REGISTERED -> "Email already registered"
        problem.setTitle(Character.toUpperCase(words.charAt(0)) + words.substring(1).replace('_', ' '));
        problem.setProperty("code", code);
        return problem;
    }
}
