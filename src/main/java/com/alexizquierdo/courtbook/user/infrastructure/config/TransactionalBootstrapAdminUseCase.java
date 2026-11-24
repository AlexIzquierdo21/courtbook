package com.alexizquierdo.courtbook.user.infrastructure.config;

import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminCommand;
import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminUseCase;
import com.alexizquierdo.courtbook.user.domain.model.User;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

class TransactionalBootstrapAdminUseCase implements BootstrapAdminUseCase {

    private final BootstrapAdminUseCase delegate;
    private final TransactionTemplate tx;

    TransactionalBootstrapAdminUseCase(BootstrapAdminUseCase delegate, PlatformTransactionManager txManager) {
        this.delegate = delegate;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public Optional<User> bootstrap(BootstrapAdminCommand command) {
        return tx.execute(status -> delegate.bootstrap(command));
    }
}
