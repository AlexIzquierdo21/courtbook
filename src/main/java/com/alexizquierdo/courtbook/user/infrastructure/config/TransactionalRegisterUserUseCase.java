package com.alexizquierdo.courtbook.user.infrastructure.config;

import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserCommand;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.domain.model.User;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionalRegisterUserUseCase implements RegisterUserUseCase {

    private final RegisterUserUseCase delegate;
    private final TransactionTemplate tx;

    TransactionalRegisterUserUseCase(RegisterUserUseCase delegate, PlatformTransactionManager txManager) {
        this.delegate = delegate;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public User register(RegisterUserCommand command) {
        return tx.execute(status -> delegate.register(command));
    }
}
