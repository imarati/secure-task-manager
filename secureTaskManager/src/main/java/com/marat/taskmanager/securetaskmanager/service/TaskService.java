package com.marat.taskmanager.securetaskmanager.service;

import com.marat.taskmanager.securetaskmanager.dto.task.CreateTaskRequest;
import com.marat.taskmanager.securetaskmanager.dto.task.TaskResponse;
import com.marat.taskmanager.securetaskmanager.dto.task.UpdateTaskRequest;
import com.marat.taskmanager.securetaskmanager.entity.enums.Role;
import com.marat.taskmanager.securetaskmanager.entity.Task;
import com.marat.taskmanager.securetaskmanager.entity.User;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskStatus;
import com.marat.taskmanager.securetaskmanager.exception.NotFoundException;
import com.marat.taskmanager.securetaskmanager.repository.TaskRepository;
import com.marat.taskmanager.securetaskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse create(CreateTaskRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TaskStatus.TODO)
                .owner(currentUser)
                .build();

        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> getMyTasks(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        return taskRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getById(Long taskId, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        checkTaskAccess(task, authentication);

        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(
            Long taskId,
            UpdateTaskRequest request,
            Authentication authentication
    ) {
        Task task = getTaskOrThrow(taskId);
        checkTaskAccess(task, authentication);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());

        return toResponse(task);
    }

    @Transactional
    public void delete(Long taskId, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        checkTaskAccess(task, authentication);

        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found: " + taskId));
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private void checkTaskAccess(Task task, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        boolean isOwner = task.getOwner().getId() == currentUser.getId();
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this task");
        }
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .ownerId(task.getOwner().getId())
                .ownerEmail(task.getOwner().getEmail())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}