
package com.example.taskManagement.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.taskManagement.dto.TaskDashboardResponse;
import com.example.taskManagement.dto.TaskRequest;
import com.example.taskManagement.dto.TaskResponse;
import com.example.taskManagement.dto.UpdateTaskRequest;
import com.example.taskManagement.entity.TaskStatus;
import com.example.taskManagement.exception.ResourceNotFoundException;
import com.example.taskManagement.service.TaskService;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    private TaskResponse sampleTask() {
        return new TaskResponse(
                1L,
                "Learn Spring Boot",
                "Prepare for Java backend interviews",
                TaskStatus.PENDING,
                LocalDate.of(2026, 12, 1),
                LocalDateTime.of(2026, 11, 15, 10, 30)
        );
    }

    // 1. CREATE TASK - SUCCESS

    @Test
    void createTaskShouldReturnCreated() throws Exception {

        when(taskService.createTask(any(TaskRequest.class)))
                .thenReturn(sampleTask());

        String requestJson = """
                {
                    "title": "Learn Spring Boot",
                    "description": "Prepare for Java backend interviews",
                    "dueDate": "2026-12-01"
                }
                """;

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));

        verify(taskService).createTask(any(TaskRequest.class));
    }

    // 2. CREATE TASK - DEFAULT STATUS IS HANDLED BY SERVICE

    @Test
    void createTaskShouldAcceptRequestWithoutStatus() throws Exception {

        when(taskService.createTask(any(TaskRequest.class)))
                .thenReturn(sampleTask());

        String requestJson = """
                {
                    "title": "Learn Java",
                    "description": "Study Java collections"
                }
                """;

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // 3. CREATE TASK - VALIDATION FAILURE

    @Test
    void createTaskShouldReturnBadRequestForBlankTitle() throws Exception {

        String requestJson = """
                {
                    "title": "",
                    "description": "Invalid task"
                }
                """;

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title")
                        .value("Task title is required"));

        verifyNoInteractions(taskService);
    }

    // 4. GET TASK BY ID - SUCCESS

    @Test
    void getTaskByIdShouldReturnTask() throws Exception {

        when(taskService.getTaskResponseById(1L))
                .thenReturn(sampleTask());

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).getTaskResponseById(1L);
    }

    // 5. GET TASK BY ID - NOT FOUND

    @Test
    void getTaskByIdShouldReturnNotFound() throws Exception {

        when(taskService.getTaskResponseById(99L))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(get("/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/tasks/99"));

        verify(taskService).getTaskResponseById(99L);
    }

    // 6. SEARCH TASKS - SUCCESS

    @Test
    void searchTasksShouldReturnPagedResults() throws Exception {

        Page<TaskResponse> taskPage = new PageImpl<>(
                List.of(sampleTask()),
                PageRequest.of(0, 10),
                1
        );

        when(taskService.searchTasks(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(0), eq(10), eq("createdAt"), eq("desc")
        )).thenReturn(taskPage);

        mockMvc.perform(get("/tasks")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Learn Spring Boot"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskService).searchTasks(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(0), eq(10), eq("createdAt"), eq("desc")
        );
    }

    // 7. SEARCH TASKS - FILTERS

    @Test
    void searchTasksShouldPassFiltersToService() throws Exception {

        Page<TaskResponse> taskPage = new PageImpl<>(
                List.of(sampleTask())
        );

        when(taskService.searchTasks(
                eq("Spring"), eq("PENDING"),
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 12, 31)),
                eq(true), eq(0), eq(5), eq("dueDate"), eq("asc")
        )).thenReturn(taskPage);

        mockMvc.perform(get("/tasks")
                .param("title", "Spring")
                .param("status", "PENDING")
                .param("dueDateFrom", "2026-11-01")
                .param("dueDateTo", "2026-12-31")
                .param("overdueOnly", "true")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "dueDate")
                .param("direction", "asc"))
                .andExpect(status().isOk());

        verify(taskService).searchTasks(
                eq("Spring"), eq("PENDING"),
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 12, 31)),
                eq(true), eq(0), eq(5), eq("dueDate"), eq("asc")
        );
    }

    // 8. SEARCH TASKS - INVALID STATUS

    @Test
    void searchTasksShouldReturnBadRequestForInvalidStatus()
            throws Exception {

        when(taskService.searchTasks(
                isNull(),
                eq("INVALID"),
                isNull(),
                isNull(),
                eq(false),
                eq(0),
                eq(10),
                eq("createdAt"),
                eq("desc")
        )).thenThrow(
                new IllegalArgumentException("Invalid task status")
        );

        mockMvc.perform(get("/tasks")
                .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid task status"));
    }

    // 9. SEARCH TASKS - INVALID PAGE SIZE

    @Test
    void searchTasksShouldReturnBadRequestForInvalidPageSize()
            throws Exception {

        when(taskService.searchTasks(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(0), eq(200), eq("createdAt"), eq("desc")
        )).thenThrow(new IllegalArgumentException(
                "Page size must be between 1 and 100"));

        mockMvc.perform(get("/tasks")
                .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page size must be between 1 and 100"));
    }

    // 10. SEARCH TASKS - INVALID DATE RANGE

    @Test
    void searchTasksShouldReturnBadRequestForInvalidDateRange()
            throws Exception {

        when(taskService.searchTasks(
                isNull(), isNull(),
                eq(LocalDate.of(2026, 12, 31)),
                eq(LocalDate.of(2026, 11, 1)),
                eq(false), eq(0), eq(10), eq("createdAt"), eq("desc")
        )).thenThrow(new IllegalArgumentException(
                "Start date cannot be after end date"));

        mockMvc.perform(get("/tasks")
                .param("dueDateFrom", "2026-12-31")
                .param("dueDateTo", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Start date cannot be after end date"));
    }

    // 11. UPDATE TASK - SUCCESS

    @Test
    void updateTaskShouldReturnUpdatedTask() throws Exception {

        when(taskService.updateTask(
                eq(1L), any(UpdateTaskRequest.class)))
                .thenReturn(sampleTask());

        String requestJson = """
                {
                    "title": "Updated Spring Boot",
                    "description": "Updated description",
                    "status": "IN_PROGRESS",
                    "dueDate": "2026-12-01"
                }
                """;

        mockMvc.perform(put("/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"));

        verify(taskService).updateTask(
                eq(1L), any(UpdateTaskRequest.class));
    }

    // 12. UPDATE TASK - VALIDATION FAILURE

    @Test
    void updateTaskShouldReturnBadRequestWhenStatusIsMissing()
            throws Exception {

        String requestJson = """
                {
                    "title": "Updated task",
                    "description": "Updated description"
                }
                """;

        mockMvc.perform(put("/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status")
                        .value("Task status is required"));

        verifyNoInteractions(taskService);
    }

    // 13. UPDATE TASK - NOT FOUND

    @Test
    void updateTaskShouldReturnNotFoundWhenTaskDoesNotExist()
            throws Exception {

        String requestJson = """
                {
                    "title": "Updated task",
                    "description": "Updated description",
                    "status": "IN_PROGRESS"
                }
                """;

        when(taskService.updateTask(
                eq(99L), any(UpdateTaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(put("/tasks/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        verify(taskService).updateTask(
                eq(99L), any(UpdateTaskRequest.class));
    }

    // 14. DELETE TASK - SUCCESS

    @Test
    void deleteTaskShouldReturnNoContent() throws Exception {

        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    // 15. DELETE TASK - NOT FOUND

    @Test
    void deleteTaskShouldReturnNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Task not found"))
                .when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        verify(taskService).deleteTask(99L);
    }

    // 16. DASHBOARD - SUCCESS

    @Test
    void getTaskDashboardShouldReturnSummary() throws Exception {

        TaskDashboardResponse dashboard =
                new TaskDashboardResponse(10, 3, 2, 5, 1);

        when(taskService.getTaskDashboard())
                .thenReturn(dashboard);

        mockMvc.perform(get("/tasks/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10))
                .andExpect(jsonPath("$.completedTasks").value(3))
                .andExpect(jsonPath("$.inProgressTasks").value(2))
                .andExpect(jsonPath("$.pendingTasks").value(5))
                .andExpect(jsonPath("$.overdueTasks").value(1));

        verify(taskService).getTaskDashboard();
    }
}
