
package com.example.taskManagement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.example.taskManagement.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser(String name, String email) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("testPasswordHash");

        return userRepository.saveAndFlush(user);
    }

    // 1. SAVE USER

    @Test
    void saveUserShouldPersistUserInDatabase() {

        User user = createUser(
                "Test User",
                "test@gmail.com"
        );

        assertThat(user.getId()).isNotNull();

        Optional<User> result =
                userRepository.findById(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test User");
        assertThat(result.get().getEmail()).isEqualTo("test@gmail.com");
    }

    // 2. FIND USER BY EMAIL - SUCCESS

    @Test
    void findByEmailShouldReturnUserWhenEmailExists() {

        createUser("Nursen", "nursen@gmail.com");

        Optional<User> result =
                userRepository.findByEmail("nursen@gmail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nursen");
    }

    // 3. FIND USER BY EMAIL - NOT FOUND

    @Test
    void findByEmailShouldReturnEmptyWhenEmailDoesNotExist() {

        Optional<User> result =
                userRepository.findByEmail("unknown@gmail.com");

        assertThat(result).isEmpty();
    }

    // 4. CHECK EMAIL EXISTS - TRUE

    @Test
    void existsByEmailShouldReturnTrueWhenEmailExists() {

        createUser("Test User", "existing@gmail.com");

        boolean result =
                userRepository.existsByEmail("existing@gmail.com");

        assertThat(result).isTrue();
    }

    // 5. CHECK EMAIL EXISTS - FALSE

    @Test
    void existsByEmailShouldReturnFalseWhenEmailDoesNotExist() {

        boolean result =
                userRepository.existsByEmail("unknown@gmail.com");

        assertThat(result).isFalse();
    }

    // 6. DELETE USER

    @Test
    void deleteUserShouldRemoveUserFromDatabase() {

        User user = createUser(
                "Delete User",
                "delete@gmail.com"
        );

        Long userId = user.getId();

        userRepository.deleteById(userId);
        userRepository.flush();

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    // 7. UPDATE USER

    @Test
    void updateUserShouldPersistUpdatedDetails() {

        User user = createUser(
                "Old Name",
                "update@gmail.com"
        );

        user.setName("Updated Name");

        userRepository.saveAndFlush(user);

        Optional<User> result =
                userRepository.findById(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Updated Name");
    }
}
