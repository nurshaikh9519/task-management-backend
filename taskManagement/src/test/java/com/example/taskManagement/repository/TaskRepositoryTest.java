
package com.example.taskManagement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.example.taskManagement.entity.Task;
import com.example.taskManagement.entity.TaskStatus;
import com.example.taskManagement.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    // Helper method to create a user

    private User createUser(String email) {

        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("testPasswordHash");

        return userRepository.saveAndFlush(user);
    }

    // Helper method to create a task

    private Task createTask(
            User user,
            String title,
            TaskStatus status,
            LocalDate dueDate) {

        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Test task description");
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setUser(user);

        return taskRepository.saveAndFlush(task);
    }

    // 1. SAVE TASK

    @Test
    void saveTaskShouldPersistTaskInDatabase() {

        User user = createUser("save@gmail.com");

        Task task = createTask(
                user,
                "Learn Java",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(5)
        );

        assertThat(task.getId()).isPositive();

        Task savedTask = taskRepository.findById(task.getId())
                .orElseThrow();

        assertThat(savedTask.getTitle()).isEqualTo("Learn Java");
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(savedTask.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedTask.getCreatedAt()).isNotNull();
    }

    // 2. FIND TASK BY ID AND USER ID

    @Test
    void findByIdAndUserIdShouldReturnTaskForCorrectUser() {

        User user = createUser("owner@gmail.com");

        Task task = createTask(
                user,
                "Spring Boot",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(3)
        );

        var result = taskRepository.findByIdAndUser_Id(
                task.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Spring Boot");
    }

    // 3. FIND TASK SHOULD NOT RETURN ANOTHER USERS TASK

    @Test
    void findByIdAndUserIdShouldNotReturnAnotherUsersTask() {

        User owner = createUser("owner2@gmail.com");
        User anotherUser = createUser("another@gmail.com");

        Task task = createTask(
                owner,
                "Private Task",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(2)
        );

        var result = taskRepository.findByIdAndUser_Id(
                task.getId(), anotherUser.getId());

        assertThat(result).isEmpty();
    }

    // 4. FIND ALL TASKS BY USER WITH PAGINATION

    @Test
    void findAllByUserIdShouldReturnPagedTasks() {

        User user = createUser("pagination@gmail.com");

        createTask(user, "Task 1", TaskStatus.PENDING,
                LocalDate.now().plusDays(1));

        createTask(user, "Task 2", TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(2));

        createTask(user, "Task 3", TaskStatus.COMPLETED,
                LocalDate.now().plusDays(3));

        Page<Task> result = taskRepository.findAllByUser_Id(
                user.getId(),
                PageRequest.of(0, 2, Sort.by("id").ascending())
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    // 5. COUNT TASKS BY USER ID

    @Test
    void countByUserIdShouldReturnTotalTasks() {

        User user = createUser("count@gmail.com");

        createTask(user, "Task 1", TaskStatus.PENDING,
                LocalDate.now().plusDays(1));

        createTask(user, "Task 2", TaskStatus.COMPLETED,
                LocalDate.now().plusDays(2));

        createTask(user, "Task 3", TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(3));

        long count = taskRepository.countByUser_Id(user.getId());

        assertThat(count).isEqualTo(3);
    }

    // 6. COUNT TASKS BY STATUS

    @Test
    void countByUserIdAndStatusShouldReturnCorrectCount() {

        User user = createUser("status@gmail.com");

        createTask(user, "Task 1", TaskStatus.PENDING,
                LocalDate.now().plusDays(1));

        createTask(user, "Task 2", TaskStatus.PENDING,
                LocalDate.now().plusDays(2));

        createTask(user, "Task 3", TaskStatus.COMPLETED,
                LocalDate.now().plusDays(3));

        long pendingCount = taskRepository.countByUser_IdAndStatus(
                user.getId(), TaskStatus.PENDING);

        long completedCount = taskRepository.countByUser_IdAndStatus(
                user.getId(), TaskStatus.COMPLETED);

        assertThat(pendingCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(1);
    }

    // 7. COUNT OVERDUE TASKS

    @Test
    void countOverdueTasksShouldExcludeCompletedTasks() {

        User user = createUser("overdue@gmail.com");

        createTask(user, "Overdue 1", TaskStatus.PENDING,
                LocalDate.now().minusDays(3));

        createTask(user, "Overdue 2", TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(2));

        createTask(user, "Completed overdue", TaskStatus.COMPLETED,
                LocalDate.now().minusDays(1));

        createTask(user, "Future task", TaskStatus.PENDING,
                LocalDate.now().plusDays(3));

        long overdueCount =
                taskRepository.countByUser_IdAndDueDateBeforeAndStatusNot(
                        user.getId(),
                        LocalDate.now(),
                        TaskStatus.COMPLETED
                );

        assertThat(overdueCount).isEqualTo(2);
    }

    // 8. SEARCH TASKS USING JPA SPECIFICATION

    @Test
    void findAllWithSpecificationShouldFilterTasksByTitle() {

        User user = createUser("search@gmail.com");

        createTask(user, "Learn Spring Boot", TaskStatus.PENDING,
                LocalDate.now().plusDays(1));

        createTask(user, "Learn Java", TaskStatus.PENDING,
                LocalDate.now().plusDays(2));

        createTask(user, "Spring Security", TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(3));

        Specification<Task> specification = (root, query, criteriaBuilder) ->
        criteriaBuilder.and(
                criteriaBuilder.equal(
                        root.get("user").get("id"),
                        user.getId()
                ),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%spring%"
                )
        );

        List<Task> result = taskRepository.findAll(specification);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder(
                        "Learn Spring Boot",
                        "Spring Security"
                );
    }

    // 9. DELETE TASK

    @Test
    void deleteTaskShouldRemoveTaskFromDatabase() {

        User user = createUser("delete@gmail.com");

        Task task = createTask(
                user,
                "Task to delete",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(2)
        );

        Long taskId = task.getId();

        taskRepository.delete(task);
        taskRepository.flush();

        assertThat(taskRepository.findById(taskId)).isEmpty();
    }
}
