package com.example.taskManagement.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.taskManagement.dto.TaskDashboardResponse;
import com.example.taskManagement.dto.TaskRequest;
import com.example.taskManagement.dto.TaskResponse;
import com.example.taskManagement.dto.UpdateTaskRequest;
import com.example.taskManagement.entity.Task;
import com.example.taskManagement.entity.TaskStatus;
import com.example.taskManagement.entity.User;
import com.example.taskManagement.exception.ResourceNotFoundException;
import com.example.taskManagement.repository.TaskRepository;
import com.example.taskManagement.repository.UserRepository;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class TaskService {
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;

	public TaskService(TaskRepository taskRepository,UserRepository userRepository) {
		super();
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
	}
	
	private User getCurrentUser() {

	    Authentication authentication =
	            SecurityContextHolder.getContext().getAuthentication();

	    String email = authentication.getName();

	    return userRepository.findByEmail(email)
	            .orElseThrow(() ->
	                new ResourceNotFoundException("User not found"));
	}

   public Task createTask(Task task) {
	   return taskRepository.save(task);
   }
   
   public List<Task> getAllTasks() {
	    return taskRepository.findAll();
	}

   public Optional<Task> gettaskById(Long id){
	   return taskRepository.findById(id);  
	}
   
   public Task updateTask(Long id,Task updatedTask) {
	   Task existingTask = taskRepository.findById(id)
			               .orElseThrow(()-> new ResourceNotFoundException("task not found by id " +id));
	    existingTask.setTitle(updatedTask.getTitle());
	    existingTask.setDescription(updatedTask.getDescription());
	    existingTask.setStatus(updatedTask.getStatus());
	    existingTask.setDueDate(updatedTask.getDueDate());

	    return taskRepository.save(existingTask);
   }
   
   public void deleteTask(Long id) {

	    // Get currently logged-in user
	    User currentUser = getCurrentUser();

	    // Verify task ownership
	    Task task = taskRepository
	            .findByIdAndUser_Id(id, currentUser.getId())
	            .orElseThrow(() ->
	                    new ResourceNotFoundException("Task not found"));

	    // Delete task
	    taskRepository.delete(task);
	}
   

public TaskResponse createTask(TaskRequest request) {

    Task task = new Task();

    task.setTitle(request.getTitle());
    task.setDescription(request.getDescription());

    task.setStatus(request.getStatus() == null
            ? TaskStatus.PENDING
            : request.getStatus());

    task.setDueDate(request.getDueDate());
    task.setUser(getCurrentUser());

    Task savedTask = taskRepository.save(task);

    return new TaskResponse(
            savedTask.getId(),
            savedTask.getTitle(),
            savedTask.getDescription(),
            savedTask.getStatus(),
            savedTask.getDueDate(),
            savedTask.getCreatedAt()
    );
}

public List<TaskResponse> getAllTaskResponses(){
	 return taskRepository.findAll()
	            .stream()
	            .map(this::convertToResponse)
	            .toList();
}

private TaskResponse convertToResponse(Task task) {

    return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getDueDate(),
            task.getCreatedAt()
    );
}

public TaskResponse getTaskResponseById(Long id) {
    User currentUser = getCurrentUser();

    Task task = taskRepository
            .findByIdAndUser_Id(id, currentUser.getId())
            .orElseThrow(() ->
                    new ResourceNotFoundException("Task not found"));

    return convertToResponse(task);

}



public TaskResponse updateTask(Long id, UpdateTaskRequest request) {

    // Get currently logged-in user
    User currentUser = getCurrentUser();

    // Find task only if it belongs to the logged-in user
    Task existingTask = taskRepository
            .findByIdAndUser_Id(id, currentUser.getId())
            .orElseThrow(() ->
                    new ResourceNotFoundException("Task not found"));

    // Update task details
    existingTask.setTitle(request.getTitle());
    existingTask.setDescription(request.getDescription());
    existingTask.setStatus(request.getStatus());
    existingTask.setDueDate(request.getDueDate());

    // Save updated task
    Task updatedTask = taskRepository.save(existingTask);

    return convertToResponse(updatedTask);
}


public Page<TaskResponse> searchTasks(
        String title,
        String status,
        LocalDate dueDateFrom,
        LocalDate dueDateTo,
        boolean overdueOnly,
        int page,
        int size,
        String sortBy,
        String direction) {

    // Validate pagination
    if (page < 0) {
        throw new IllegalArgumentException(
                "Page number cannot be negative");
    }

    if (size < 1 || size > 100) {
        throw new IllegalArgumentException(
                "Page size must be between 1 and 100");
    }

    // Validate date range
    if (dueDateFrom != null && dueDateTo != null
            && dueDateFrom.isAfter(dueDateTo)) {
        throw new IllegalArgumentException(
                "Start date cannot be after end date");
    }

    // Validate sorting field
    Set<String> allowedSortFields = Set.of(
            "id", "title", "status", "dueDate", "createdAt"
    );

    if (!allowedSortFields.contains(sortBy)) {
        throw new IllegalArgumentException(
                "Invalid sorting field");
    }

    // Get logged-in user
    User currentUser = getCurrentUser();

    // Default sorting direction
    Sort.Direction sortDirection =
            "asc".equalsIgnoreCase(direction)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(sortDirection, sortBy)
    );

    // Always restrict results to logged-in user
    Specification<Task> spec = (root, query, cb) ->
            cb.equal(
                    root.get("user").get("id"),
                    currentUser.getId()
            );

    // Search by title
    if (title != null && !title.isBlank()) {
        spec = spec.and((root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                ));
    }

    // Filter by status
    if (status != null && !status.isBlank()) {

        TaskStatus taskStatus;

        try {
            taskStatus = TaskStatus.valueOf(
                    status.toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid task status");
        }

        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), taskStatus));
    }

    // Filter due date from
    if (dueDateFrom != null) {
        spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(
                        root.get("dueDate"),
                        dueDateFrom
                ));
    }

    // Filter due date to
    if (dueDateTo != null) {
        spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(
                        root.get("dueDate"),
                        dueDateTo
                ));
    }

    // Filter overdue tasks
    if (overdueOnly) {
        spec = spec.and((root, query, cb) ->
                cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.lessThan(
                                root.get("dueDate"),
                                LocalDate.now()
                        ),
                        cb.notEqual(
                                root.get("status"),
                                TaskStatus.COMPLETED
                        )
                ));
    }

    // Execute query
    Page<Task> taskPage =
            taskRepository.findAll(spec, pageable);

    // Convert entities into DTOs
    return taskPage.map(this::convertToResponse);
}


public TaskDashboardResponse getTaskDashboard() {

    // Get logged-in user
    User currentUser = getCurrentUser();

    Long userId = currentUser.getId();

    // Calculate total tasks
    long totalTasks =
            taskRepository.countByUser_Id(userId);

    // Calculate completed tasks
    long completedTasks =
            taskRepository.countByUser_IdAndStatus(
                    userId,
                    TaskStatus.COMPLETED
            );

    // Calculate in-progress tasks
    long inProgressTasks =
            taskRepository.countByUser_IdAndStatus(
                    userId,
                    TaskStatus.IN_PROGRESS
            );

    // Calculate pending tasks
    long pendingTasks =
            taskRepository.countByUser_IdAndStatus(
                    userId,
                    TaskStatus.PENDING
            );

    // Calculate overdue tasks
    long overdueTasks =
            taskRepository
                .countByUser_IdAndDueDateBeforeAndStatusNot(
                    userId,
                    LocalDate.now(),
                    TaskStatus.COMPLETED
                );

    return new TaskDashboardResponse(
            totalTasks,
            completedTasks,
            inProgressTasks,
            pendingTasks,
            overdueTasks
    );
}


}
