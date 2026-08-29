package com.marat.taskmanager.securetaskmanager.controller;

import com.marat.taskmanager.securetaskmanager.config.PaginationProperties;
import com.marat.taskmanager.securetaskmanager.dto.task.CreateTaskRequest;
import com.marat.taskmanager.securetaskmanager.dto.task.TaskResponse;
import com.marat.taskmanager.securetaskmanager.dto.task.UpdateTaskRequest;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskPriority;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskStatus;
import com.marat.taskmanager.securetaskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final PaginationProperties paginationProperties;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "title",
            "status",
            "priority"
    );

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
    public ResponseEntity<Page<TaskResponse>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication
    ) {
        int requestedSize = size == null
                ? paginationProperties.getDefaultPageSize()
                : size;

        Pageable pageable = createSafePageable(
                page,
                requestedSize,
                sort
        );

        return ResponseEntity.ok(
                taskService.getMyTasks(
                        status,
                        priority,
                        pageable,
                        authentication
                )
        );
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
        return ResponseEntity.ok(
                taskService.update(taskId, request, authentication)
        );
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        taskService.delete(taskId, authentication);

        return ResponseEntity.noContent().build();
    }

    private Pageable createSafePageable(
            int page,
            int size,
            String sortParameter
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be greater than or equal to zero"
            );
        }

        if (size < 1 || size > paginationProperties.getMaxPageSize()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and "
                            + paginationProperties.getMaxPageSize()
            );
        }

        String[] sortParts = sortParameter.split(",", -1);

        if (sortParts.length != 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort must have format field,direction"
            );
        }

        String sortField = sortParts[0].trim();
        String sortDirection = sortParts[1].trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sorting by this field is not allowed"
            );
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort direction must be asc or desc"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );
    }
}