package com.alexizquierdo.courtbook.user.infrastructure.rest;

import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserCommand;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidEmailException;
import com.alexizquierdo.courtbook.user.domain.exception.InvalidPasswordException;
import com.alexizquierdo.courtbook.user.domain.exception.UserNotFoundException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String PASSWORD = "Secret1!";
    private static final PasswordHash HASH = new PasswordHash("$2a$10$abcdefghijklmnopqrstuv");
    private static final User ANA = User.create("Ana", "López", Email.of("ana@x.com"), HASH);

    // "role" is not part of the contract: it must be ignored, not rejected.
    private static final String REGISTER_JSON = """
            { "firstName": "Ana", "lastName": "López", "email": "ana@x.com",
              "password": "%s", "role": "ADMIN" }
            """.formatted(PASSWORD);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private GetUserQuery getUserQuery;

    private MvcTestResult postRegister(String json) {
        return mvc.post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    @Test
    void registerReturns201WithLocationAndTheUserWithoutPassword() {
        when(registerUserUseCase.register(any())).thenReturn(ANA);

        MvcTestResult result = postRegister(REGISTER_JSON);

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/v1/users/" + ANA.getId().value());
        assertThat(result).bodyJson().isStrictlyEqualTo("""
                { "id": "%s", "firstName": "Ana", "lastName": "López", "email": "ana@x.com", "role": "PLAYER" }
                """.formatted(ANA.getId().value()));
        assertThat(result).bodyText()
                .doesNotContainIgnoringCase("password")
                .doesNotContain(PASSWORD)
                .doesNotContain(HASH.value());
        verify(registerUserUseCase).register(new RegisterUserCommand("Ana", "López", "ana@x.com", PASSWORD));
    }

    @Test
    void registerReturns400WithErrorsWhenTheBodyIsInvalid() {
        MvcTestResult result = postRegister("""
                { "firstName": " ", "lastName": "López", "password": "%s" }
                """.formatted(PASSWORD));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("firstName", "email");
        assertThat(result).bodyText().doesNotContain(PASSWORD);
        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void registerReturns409WhenTheEmailIsAlreadyRegistered() {
        when(registerUserUseCase.register(any())).thenThrow(new EmailAlreadyRegisteredException());

        MvcTestResult result = postRegister(REGISTER_JSON);

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {
                  "type": "https://courtbook.dev/errors/email-already-registered",
                  "title": "Email already registered",
                  "status": 409,
                  "detail": "Email is already registered",
                  "instance": "/api/v1/users",
                  "code": "EMAIL_ALREADY_REGISTERED"
                }
                """);
    }

    @Test
    void registerReturns400WithTheValueObjectCode() {
        when(registerUserUseCase.register(any()))
                .thenThrow(new InvalidEmailException())
                .thenThrow(new InvalidPasswordException("Plain password must contain at least one digit"));

        MvcTestResult invalidEmail = postRegister(REGISTER_JSON);
        MvcTestResult invalidPassword = postRegister(REGISTER_JSON);

        assertThat(invalidEmail).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalidEmail).bodyJson().extractingPath("$.code").isEqualTo("INVALID_EMAIL");
        assertThat(invalidPassword).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalidPassword).bodyJson().extractingPath("$.code").isEqualTo("INVALID_PASSWORD");
        assertThat(invalidPassword).bodyText().doesNotContain(PASSWORD);
    }

    @Test
    void getReturnsTheUserWithoutPassword() {
        when(getUserQuery.getUserById(ANA.getId())).thenReturn(ANA);

        MvcTestResult result = mvc.get().uri("/api/v1/users/{id}", ANA.getId().value()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("ana@x.com");
        assertThat(result).bodyText().doesNotContainIgnoringCase("password").doesNotContain(HASH.value());
    }

    @Test
    void getReturns404WhenTheUserDoesNotExist() {
        when(getUserQuery.getUserById(ANA.getId())).thenThrow(new UserNotFoundException(ANA.getId()));

        MvcTestResult result = mvc.get().uri("/api/v1/users/{id}", ANA.getId().value()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void getReturns400WhenTheIdIsNotAUuid() {
        MvcTestResult result = mvc.get().uri("/api/v1/users/not-a-uuid").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(getUserQuery);
    }
}
