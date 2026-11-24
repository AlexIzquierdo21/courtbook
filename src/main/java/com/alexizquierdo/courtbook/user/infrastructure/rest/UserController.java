package com.alexizquierdo.courtbook.user.infrastructure.rest;

import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import com.alexizquierdo.courtbook.user.infrastructure.rest.dto.RegisterUserRequest;
import com.alexizquierdo.courtbook.user.infrastructure.rest.dto.UserResponse;
import com.alexizquierdo.courtbook.user.infrastructure.rest.mapper.UserRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(UserController.BASE_PATH)
public class UserController {

    static final String BASE_PATH = "/api/v1/users";

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserQuery getUserQuery;

    public UserController(RegisterUserUseCase registerUserUseCase, GetUserQuery getUserQuery) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserQuery = getUserQuery;
    }

    // Public endpoint: no X-User-Id.
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = registerUserUseCase.register(UserRestMapper.toCommand(request));
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + user.getId().value()))
                .body(UserRestMapper.toResponse(user));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return UserRestMapper.toResponse(getUserQuery.getUserById(UserId.of(id)));
    }
}
