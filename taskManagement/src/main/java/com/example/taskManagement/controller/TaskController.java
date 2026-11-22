package com.example.taskManagement.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskManagement.dto.TaskDashboardResponse;
import com.example.taskManagement.dto.TaskRequest;
import com.example.taskManagement.dto.TaskResponse;
import com.example.taskManagement.dto.UpdateTaskRequest;
import com.example.taskManagement.entity.Task;
import com.example.taskManagement.repository.TaskRepository;
import com.example.taskManagement.service.TaskService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.taskManagement.entity.TaskStatus;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tasks")
public class TaskController {
	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		super();
		this.taskService = taskService;
	}	
	
	@PostMapping
	public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest task){
		TaskResponse saveTask = taskService.createTask(task);
		return ResponseEntity.status(201).body(saveTask);
	}
	
//	@GetMapping
//	public List<Task> getAllTasks() {
//	    return taskService.getAllTasks();
//	}
	
//	@GetMapping
//	public ResponseEntity<List<TaskResponse>> getAllTasks() {
//
//	    return ResponseEntity.ok(
//	            taskService.getAllTaskResponses()
//	    );
//	}
	

@GetMapping
public ResponseEntity<Page<TaskResponse>> searchTasks(

        @RequestParam(name = "title", required = false)
        String title,

        @RequestParam(name = "status", required = false)
        String status,

        @RequestParam(name = "dueDateFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dueDateFrom,

        @RequestParam(name = "dueDateTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dueDateTo,

        @RequestParam(name = "overdueOnly", defaultValue = "false")
        boolean overdueOnly,

        @RequestParam(name = "page", defaultValue = "0")
        int page,

        @RequestParam(name = "size", defaultValue = "10")
        int size,

        @RequestParam(name = "sortBy", defaultValue = "createdAt")
        String sortBy,

        @RequestParam(name = "direction", defaultValue = "desc")
        String direction) {

    Page<TaskResponse> response = taskService.searchTasks(
            title,
            status,
            dueDateFrom,
            dueDateTo,
            overdueOnly,
            page,
            size,
            sortBy,
            direction
    );

    return ResponseEntity.ok(response);
}

	
//	@GetMapping("/{id}")
//	public ResponseEntity<Task> getTaskById(@PathVariable("id") Long id){
//		return taskService.gettaskById(id)
//				.map(ResponseEntity::ok)
//				.orElseGet(()->ResponseEntity.notFound().build());
//				}
	@GetMapping("/{id}")
	public ResponseEntity<TaskResponse> getTaskById(
	        @PathVariable("id") Long id) {

	    return ResponseEntity.ok(
	            taskService.getTaskResponseById(id)
	    );
	}
	
//	@PutMapping("/{id}")
//	public ResponseEntity<Task> updateTaskById(@PathVariable("id")Long id, @RequestBody Task task){
//	Task updatedTask = taskService.updateTask(id, task);	
//	return ResponseEntity.ok(updatedTask);
//	}
	
	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> updateTask(
	        @PathVariable("id") Long id,
	        @Valid @RequestBody UpdateTaskRequest request) {

	    return ResponseEntity.ok(
	            taskService.updateTask(id, request)
	    );
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void>deleteById(@PathVariable("id")Long id){
		taskService.deleteTask(id);
		return ResponseEntity.noContent().build();
	}
	

    @GetMapping("/dashboard")
	public ResponseEntity<TaskDashboardResponse> getTaskDashboard() {
	
	    TaskDashboardResponse dashboard =
	            taskService.getTaskDashboard();
	
	    return ResponseEntity.ok(dashboard);
	}

 
}
