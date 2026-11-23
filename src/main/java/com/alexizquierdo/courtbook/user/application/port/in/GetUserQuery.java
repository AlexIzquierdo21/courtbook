package com.alexizquierdo.courtbook.user.application.port.in;

import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;

public interface GetUserQuery {

    User getUserById(UserId userId);
}
