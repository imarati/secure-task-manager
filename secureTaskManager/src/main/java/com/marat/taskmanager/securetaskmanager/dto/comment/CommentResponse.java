package com.marat.taskmanager.securetaskmanager.dto.comment;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CommentResponse {

    private Long id;
    private Long taskId;

    private Long authorId;
    private String authorEmail;

    private String text;
    private Instant createdAt;
}