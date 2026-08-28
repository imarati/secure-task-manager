package com.marat.taskmanager.securetaskmanager.controller;

import com.marat.taskmanager.securetaskmanager.dto.comment.CommentResponse;
import com.marat.taskmanager.securetaskmanager.dto.comment.CreateCommentRequest;
import com.marat.taskmanager.securetaskmanager.service.CommentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        CommentResponse response = commentService.create(taskId, request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAllByTaskId(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                commentService.getAllByTaskId(taskId, authentication)
        );
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        commentService.delete(taskId, commentId, authentication);

        return ResponseEntity.noContent().build();
    }
}