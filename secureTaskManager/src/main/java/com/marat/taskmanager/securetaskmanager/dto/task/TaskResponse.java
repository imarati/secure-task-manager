package com.marat.taskmanager.securetaskmanager.dto.task;

import com.marat.taskmanager.securetaskmanager.entity.enums.TaskPriority;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;

    private Long ownerId;
    private String ownerEmail;

    private Instant createdAt;
    private Instant updatedAt;
}