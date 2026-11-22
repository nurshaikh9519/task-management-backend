package com.example.taskManagement.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.taskManagement.entity.Task;
import com.example.taskManagement.entity.TaskStatus;
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>,JpaSpecificationExecutor<Task> {
	Optional<Task> findByIdAndUser_Id(Long id, Long userId);
	
	Page<Task> findAllByUser_Id(Long userId, Pageable pageable);
	

	long countByUser_Id(Long userId);
	
	long countByUser_IdAndStatus(
	        Long userId,
	        TaskStatus status
	);
	
	long countByUser_IdAndDueDateBeforeAndStatusNot(
	        Long userId,
	        LocalDate dueDate,
	        TaskStatus status
	);

}
