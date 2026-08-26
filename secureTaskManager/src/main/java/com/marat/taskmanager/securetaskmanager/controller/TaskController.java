package com.marat.taskmanager.securetaskmanager.controller;

import com.marat.taskmanager.securetaskmanager.dto.task.CreateTaskRequest;
import com.marat.taskmanager.securetaskmanager.dto.task.TaskResponse;
import com.marat.taskmanager.securetaskmanager.dto.task.UpdateTaskRequest;
import com.marat.taskmanager.securetaskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        TaskResponse response = taskService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.getMyTasks(authentication));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getById(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.getById(taskId, authentication));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.update(taskId, request, authentication));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        taskService.delete(taskId, authentication);
        return ResponseEntity.noContent().build();
    }
}