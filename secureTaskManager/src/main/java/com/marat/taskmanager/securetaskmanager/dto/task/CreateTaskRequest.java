package com.marat.taskmanager.securetaskmanager.dto.task;

import com.marat.taskmanager.securetaskmanager.entity.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 30, message = "Title must be at most 30 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;
}