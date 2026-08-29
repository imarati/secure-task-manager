package com.marat.taskmanager.securetaskmanager.service;

import com.marat.taskmanager.securetaskmanager.dto.comment.CommentResponse;
import com.marat.taskmanager.securetaskmanager.dto.comment.CreateCommentRequest;
import com.marat.taskmanager.securetaskmanager.entity.Comment;
import com.marat.taskmanager.securetaskmanager.entity.enums.Role;
import com.marat.taskmanager.securetaskmanager.entity.Task;
import com.marat.taskmanager.securetaskmanager.entity.User;
import com.marat.taskmanager.securetaskmanager.exception.NotFoundException;
import com.marat.taskmanager.securetaskmanager.repository.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse create(
            Long taskId,
            CreateCommentRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);

        checkTaskAccess(task, currentUser);

        Comment comment = Comment.builder()
                .text(request.getText())
                .task(task)
                .author(currentUser)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void delete(
            Long taskId,
            Long commentId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Task task = getTaskOrThrow(taskId);
        Comment comment = getCommentOrThrow(commentId);

        checkCommentAccess(comment, currentUser);
        checkCommentTaskApply(comment, task);

        commentRepository.delete(comment);
    }

    public List<CommentResponse> getAllByTaskId(
            Long taskId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);

        checkTaskAccess(task, currentUser);

        return commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found: " + taskId));
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private void checkTaskAccess(Task task, User currentUser) {
        boolean isOwner = task.getOwner().getId() == currentUser.getId();
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this task");
        }
    }

    private void checkCommentAccess(Comment comment, User currentUser) {
        boolean isOwner = comment.getAuthor().getId() == currentUser.getId();
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this comment");
        }
    }

    private void checkCommentTaskApply(Comment comment, Task task) {
        if (!comment.getTask().getId().equals(task.getId())) {
            throw new NotFoundException("Comment not found in this task");
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .authorId(comment.getAuthor().getId())
                .authorEmail(comment.getAuthor().getEmail())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}