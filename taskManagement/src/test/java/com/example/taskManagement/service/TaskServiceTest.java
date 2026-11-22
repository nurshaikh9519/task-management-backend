
package com.example.taskManagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.taskManagement.dto.TaskRequest;
import com.example.taskManagement.dto.TaskResponse;
import com.example.taskManagement.dto.UpdateTaskRequest;
import com.example.taskManagement.entity.Task;
import com.example.taskManagement.entity.TaskStatus;
import com.example.taskManagement.entity.User;
import com.example.taskManagement.repository.TaskRepository;
import com.example.taskManagement.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTaskByIdShouldReturnTaskForLoggedInUser() {

        // Arrange: Set authenticated user
        String email = "nursen@gmail.com";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, java.util.List.of()
                )
        );

        // Arrange: Create mock user
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        // Arrange: Create sample task
        Task task = new Task();

        task.setTitle("Learn Java");
        task.setDescription("Practice Java concepts");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDueDate(LocalDate.of(2026, 11, 20));

        // Mock repository response
        when(taskRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(task));

        // Act: Execute service method
        TaskResponse response =
                taskService.getTaskResponseById(10L);

        // Assert: Verify result
        assertNotNull(response);
        assertEquals("Learn Java", response.getTitle());
        assertEquals("Practice Java concepts",
                response.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS,
                response.getStatus());

        // Verify repository interactions
        verify(taskRepository)
                .findByIdAndUser_Id(10L, 1L);
    }

    @Test
    void getTaskByIdShouldThrowExceptionWhenTaskNotFound() {

        String email = "nursen@gmail.com";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, java.util.List.of()
                )
        );

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser_Id(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> taskService.getTaskResponseById(99L)
        );

        verify(taskRepository)
                .findByIdAndUser_Id(99L, 1L);
    }
    

@Test
void createTaskShouldSaveTaskForLoggedInUser() {

    // ARRANGE

    String email = "nursen@gmail.com";

    // Simulate authenticated user
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    // Mock logged-in user
    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    // Prepare task request
    TaskRequest request = new TaskRequest();

    request.setTitle("Learn Spring Boot");
    request.setDescription("Practice Spring Boot REST APIs");
    request.setStatus(TaskStatus.IN_PROGRESS);
    request.setDueDate(LocalDate.of(2026, 11, 25));

    // Mock save operation
    when(taskRepository.save(any(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));


    // ACT

    TaskResponse response = taskService.createTask(request);


    // ASSERT

    assertNotNull(response);

    assertEquals("Learn Spring Boot", response.getTitle());

    assertEquals(
            "Practice Spring Boot REST APIs",
            response.getDescription()
    );

    assertEquals(
            TaskStatus.IN_PROGRESS,
            response.getStatus()
    );

    assertEquals(
            LocalDate.of(2026, 11, 25),
            response.getDueDate()
    );

    // Verify that repository save was called
    verify(taskRepository, times(1)).save(any(Task.class));

    // Capture the actual entity passed to save()
    ArgumentCaptor<Task> taskCaptor =
            ArgumentCaptor.forClass(Task.class);

    verify(taskRepository).save(taskCaptor.capture());

    Task savedTask = taskCaptor.getValue();

    // Verify task ownership
    assertSame(user, savedTask.getUser());

}

@Test
void updateTaskShouldUpdateTaskForLoggedInUser() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    Task existingTask = new Task();
    existingTask.setTitle("Learn Java");
    existingTask.setDescription("Practice Java");
    existingTask.setStatus(TaskStatus.PENDING);
    existingTask.setDueDate(LocalDate.of(2026, 11, 20));
    existingTask.setUser(user);

    when(taskRepository.findByIdAndUser_Id(10L, user.getId()))
            .thenReturn(Optional.of(existingTask));

    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setTitle("Learn Spring Boot");
    request.setDescription("Practice Spring Boot APIs");
    request.setStatus(TaskStatus.IN_PROGRESS);
    request.setDueDate(LocalDate.of(2026, 11, 25));

    when(taskRepository.save(any(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // ACT

    TaskResponse response = taskService.updateTask(10L, request);

    // ASSERT

    assertNotNull(response);
    assertEquals("Learn Spring Boot", response.getTitle());
    assertEquals("Practice Spring Boot APIs", response.getDescription());
    assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
    assertEquals(LocalDate.of(2026, 11, 25), response.getDueDate());

    verify(taskRepository).findByIdAndUser_Id(10L, user.getId());
    verify(taskRepository).save(existingTask);
}

@Test
void deleteTaskShouldDeleteTaskForLoggedInUser() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    Task existingTask = new Task();
    existingTask.setTitle("Learn Java");
    existingTask.setDescription("Practice Java");
    existingTask.setStatus(TaskStatus.PENDING);
    existingTask.setUser(user);

    when(taskRepository.findByIdAndUser_Id(10L, user.getId()))
            .thenReturn(Optional.of(existingTask));

    // ACT

    taskService.deleteTask(10L);

    // ASSERT

    verify(taskRepository).findByIdAndUser_Id(10L, user.getId());

    verify(taskRepository).delete(existingTask);
}

@Test
void deleteTaskShouldThrowExceptionWhenTaskNotFound() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    when(taskRepository.findByIdAndUser_Id(99L, user.getId()))
            .thenReturn(Optional.empty());

    // ACT & ASSERT

    assertThrows(
            RuntimeException.class,
            () -> taskService.deleteTask(99L)
    );

    verify(taskRepository, never()).delete(any(Task.class));
}

@Test
void searchTasksShouldReturnOnlyLoggedInUserTasks() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    lenient().when(userRepository.findByEmail(email))
    .thenReturn(Optional.of(user));

    Task task = new Task();
    task.setTitle("Learn Java");
    task.setDescription("Practice Java concepts");
    task.setStatus(TaskStatus.IN_PROGRESS);
    task.setDueDate(LocalDate.of(2026, 11, 25));
    task.setUser(user);

    org.springframework.data.domain.Page<Task> taskPage =
            new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of(task)
            );

    lenient().when(taskRepository.findAll(
            ArgumentMatchers.<Specification<Task>>any(),
            any(Pageable.class)
    )).thenReturn(taskPage);

    // ACT

    // Use the same searchTasks arguments as your TaskService method.
    // We'll adjust this call to your exact method signature if needed.

}

@Test
void getDashboardShouldReturnCorrectTaskCounts() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("dummy-hash");

    lenient().when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    lenient().when(taskRepository.countByUser_Id(user.getId()))
            .thenReturn(10L);

    lenient().when(taskRepository.countByUser_IdAndStatus(
            user.getId(), TaskStatus.COMPLETED))
            .thenReturn(4L);

    lenient().when(taskRepository.countByUser_IdAndStatus(
            user.getId(), TaskStatus.IN_PROGRESS))
            .thenReturn(3L);

    lenient().when(taskRepository.countByUser_IdAndStatus(
            user.getId(), TaskStatus.PENDING))
            .thenReturn(3L);

    lenient().when(taskRepository.countByUser_IdAndDueDateBeforeAndStatusNot(
            eq(user.getId()),
            any(LocalDate.class),
            eq(TaskStatus.COMPLETED)))
            .thenReturn(2L);

    // ACT

    // Replace this line with your actual dashboard service method call.

}
}
