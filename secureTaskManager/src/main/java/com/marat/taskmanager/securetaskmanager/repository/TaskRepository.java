package com.marat.taskmanager.securetaskmanager.repository;

import com.marat.taskmanager.securetaskmanager.entity.Task;
import com.marat.taskmanager.securetaskmanager.entity.User;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskPriority;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwner(User user, Pageable pageable);
    Page<Task> findByOwnerAndStatus(User user, TaskStatus status, Pageable pageable);
    Page<Task> findByOwnerAndPriority(User user, TaskPriority priority, Pageable pageable);
    Page<Task> findByOwnerAndStatusAndPriority(User user, TaskStatus status, TaskPriority priority, Pageable pageable);
}