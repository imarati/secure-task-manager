package com.marat.taskmanager.securetaskmanager.repository;

import com.marat.taskmanager.securetaskmanager.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);
}