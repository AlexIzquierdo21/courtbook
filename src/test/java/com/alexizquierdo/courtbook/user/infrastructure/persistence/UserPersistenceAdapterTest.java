package com.alexizquierdo.courtbook.user.infrastructure.persistence;

import com.alexizquierdo.courtbook.TestcontainersConfiguration;
import com.alexizquierdo.courtbook.user.domain.exception.EmailAlreadyRegisteredException;
import com.alexizquierdo.courtbook.user.domain.model.User;
import com.alexizquierdo.courtbook.user.domain.valueobject.Email;
import com.alexizquierdo.courtbook.user.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, UserPersistenceAdapter.class})
class UserPersistenceAdapterTest {

    private static final PasswordHash HASH = new PasswordHash("$2a$10$abcdefghijklmnopqrstuv");

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    // Flush and clear so the next read hits MySQL, not the persistence context cache.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savesAndFindsByIdWithTheSameUuid() {
        User user = User.create("Ana", "López", Email.of("ana@x.com"), HASH);

        adapter.save(user);
        flushAndClear();

        User found = adapter.findById(user.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getFirstName()).isEqualTo("Ana");
        assertThat(found.getLastName()).isEqualTo("López");
        assertThat(found.getEmail()).isEqualTo(user.getEmail());
        assertThat(found.getPasswordHash()).isEqualTo(HASH);
        assertThat(found.getRole()).isEqualTo(user.getRole());
    }

    @Test
    void existsByEmailIgnoresCase() {
        adapter.save(User.create("Ana", "López", Email.of("ana@x.com"), HASH));
        flushAndClear();

        assertThat(adapter.existsByEmail(Email.of("ANA@X.COM"))).isTrue();
        assertThat(adapter.existsByEmail(Email.of("other@x.com"))).isFalse();
        assertThat(adapter.findByEmail(Email.of("Ana@x.com"))).isPresent();
    }

    // Two registrations racing past existsByEmail: the unique key decides, and the loser gets the 409 exception.
    @Test
    void savingAnEmailThatIsAlreadyStoredThrowsEmailAlreadyRegistered() {
        adapter.save(User.create("Ana", "López", Email.of("ana@x.com"), HASH));

        assertThatThrownBy(() -> adapter.save(User.create("Ana", "Otra", Email.of("ANA@x.com"), HASH)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void existsAdminIsTrueOnlyWhenAnAdminIsStored() {
        adapter.save(User.create("Ana", "López", Email.of("ana@x.com"), HASH));
        flushAndClear();
        assertThat(adapter.existsAdmin()).isFalse();

        adapter.save(User.createAdmin("Root", "Admin", Email.of("admin@x.com"), HASH));
        flushAndClear();
        assertThat(adapter.existsAdmin()).isTrue();
    }
}
