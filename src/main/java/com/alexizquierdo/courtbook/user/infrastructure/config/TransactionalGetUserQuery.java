package com.alexizquierdo.courtbook.user.infrastructure.config;

import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.UserId;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionalGetUserQuery implements GetUserQuery {

    private final GetUserQuery delegate;
    private final TransactionTemplate tx;

    TransactionalGetUserQuery(GetUserQuery delegate, PlatformTransactionManager txManager) {
        this.delegate = delegate;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setReadOnly(true);
    }

    @Override
    public User getUserById(UserId userId) {
        return tx.execute(status -> delegate.getUserById(userId));
    }
}
