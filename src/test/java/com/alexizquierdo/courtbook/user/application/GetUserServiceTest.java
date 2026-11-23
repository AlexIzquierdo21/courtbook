package com.alexizquierdo.courtbook.user.application;

import com.alexizquierdo.courtbook.user.application.usecase.GetUserService;
import com.alexizquierdo.courtbook.user.domain.exception.UserNotFoundException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetUserServiceTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final GetUserService service = new GetUserService(repository);

    @Test
    void returnsTheStoredUser() {
        User user = repository.save(User.create("Ana", "López", Email.of("ana@x.com"), new PasswordHash("hashed:Secret1!")));

        assertThat(service.getUserById(user.getId())).isEqualTo(user);
    }

    @Test
    void throwsUserNotFoundForAnUnknownId() {
        assertThatThrownBy(() -> service.getUserById(UserId.generate()))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((UserNotFoundException) e).code())
                .isEqualTo("USER_NOT_FOUND");
    }
}
