package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

final class UserEntityMapper {
    private UserEntityMapper() {
    }

    static UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail().value(),
                user.getPasswordHash().value(),
                user.getRole()
        );
    }
    static User toDomain(UserEntity entity) {
        return User.restore(
                UserId.of(entity.getId()),
                entity.getFirstName(),
                entity.getLastName(),
                Email.of(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.getRole()
        );
    }
}
