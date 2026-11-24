package com.alexizquierdo.courtbook.user.infrastructure.config;

import com.alexizquierdo.courtbook.user.application.port.in.BootstrapAdminUseCase;
import com.alexizquierdo.courtbook.user.application.port.in.GetUserQuery;
import com.alexizquierdo.courtbook.user.application.port.in.RegisterUserUseCase;
import com.alexizquierdo.courtbook.user.application.port.out.PasswordHasher;
import com.alexizquierdo.courtbook.user.application.port.out.UserRepository;
import com.alexizquierdo.courtbook.user.application.usecase.BootstrapAdminService;
import com.alexizquierdo.courtbook.user.application.usecase.GetUserService;
import com.alexizquierdo.courtbook.user.application.usecase.RegisterUserService;
import com.alexizquierdo.courtbook.user.infrastructure.bootstrap.AdminBootstrapRunner;
import com.alexizquierdo.courtbook.user.infrastructure.bootstrap.BootstrapAdminProperties;
import com.alexizquierdo.courtbook.user.infrastructure.persistence.SpringDataUserRepository;
import com.alexizquierdo.courtbook.user.infrastructure.persistence.UserPersistenceAdapter;
import com.alexizquierdo.courtbook.user.infrastructure.security.BCryptPasswordHasher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

// TransactionTemplate over @Transactional: explicit transaction boundary, no proxies, decorators stays plain classes
@Configuration
@EnableConfigurationProperties(BootstrapAdminProperties.class)
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

    @Bean
    BootstrapAdminUseCase bootstrapAdminUseCase(UserRepository userRepository,
                                                PasswordHasher passwordHasher,
                                                PlatformTransactionManager txManager) {
        return new TransactionalBootstrapAdminUseCase(new BootstrapAdminService(userRepository, passwordHasher), txManager);
    }

    @Bean
    AdminBootstrapRunner adminBootstrapRunner(BootstrapAdminUseCase bootstrapAdminUseCase,
                                              BootstrapAdminProperties properties) {
        return new AdminBootstrapRunner(bootstrapAdminUseCase, properties);
    }
}
