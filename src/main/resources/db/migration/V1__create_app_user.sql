CREATE TABLE app_user (
                          id            BINARY(16)   NOT NULL,
                          first_name    VARCHAR(100) NOT NULL,
                          last_name     VARCHAR(100) NOT NULL,
                          email         VARCHAR(255) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          role          VARCHAR(20)  NOT NULL,
                          version       BIGINT       NOT NULL DEFAULT 0,
                          created_at    DATETIME(6)  NOT NULL,
                          CONSTRAINT pk_app_user PRIMARY KEY (id),
                          CONSTRAINT uk_app_user_email UNIQUE (email),
                          CONSTRAINT ck_app_user_role CHECK (role IN ('PLAYER', 'ADMIN'))
);
