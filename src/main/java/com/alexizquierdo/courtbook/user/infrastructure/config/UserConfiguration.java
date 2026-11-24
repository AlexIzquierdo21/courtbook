package com.alexizquierdo.courtbook.user.infrastructure.config;

import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.application.usecase.GetUserService;
import com.alexizquierdo.courtbook.user.application.usecase.RegisterUserService;
import com.alexizquierdo.courtbook.user.infrastructure.persistence.SpringDataUserRepository;
import com.alexizquierdo.courtbook.user.infrastructure.persistence.UserPersistenceAdapter;
import com.alexizquierdo.courtbook.user.infrastructure.security.BCryptPasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

// TransactionTemplate over @Transactional: explicit transaction boundary, no proxies, decorators stays plain classes
@Configuration
public class UserConfiguration {

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    UserRepository userRepository(SpringDataUserRepository springDataUserRepository) {
        return new UserPersistenceAdapter(springDataUserRepository);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository userRepository,
                                            PasswordHasher passwordHasher,
                                            PlatformTransactionManager txManager) {
        RegisterUserService registerUserService = new RegisterUserService(userRepository, passwordHasher);
        return new TransactionalRegisterUserUseCase(registerUserService, txManager);
    }

    @Bean
    GetUserQuery getUserQuery(UserRepository userRepository, PlatformTransactionManager txManager) {
        return new TransactionalGetUserQuery(new GetUserService(userRepository), txManager);
    }
}
