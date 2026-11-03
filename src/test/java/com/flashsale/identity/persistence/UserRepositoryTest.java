package com.flashsale.identity.persistence;

import com.flashsale.identity.domain.Role;
import com.flashsale.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Arrange
        String email = "test-" + UUID.randomUUID() + "@example.com";

        User user = User.create(email, "test-hashed-password");

        // Act
        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findByEmail(email);

        // Assert
        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldCheckIfUserExistsByEmail() {
        // Arrange
        String email = "test-" + UUID.randomUUID() + "@example.com";

        User user = User.create(email, "test-hashed-password");

        userRepository.save(user);

        // Act & Assert
        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void shouldSaveUserWithRole() {
        // Arrange
        String email = "test-" + UUID.randomUUID() + "@example.com";

        Role role = new Role("TEST_ROLE");
        roleRepository.save(role);

        User user = User.create(email, "test-hashed-password");
        user.addRole(role);

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        Optional<User> foundUser = userRepository.findByEmail(email);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRoles())
                .extracting(Role::getName)
                .contains("TEST_ROLE");
    }
}