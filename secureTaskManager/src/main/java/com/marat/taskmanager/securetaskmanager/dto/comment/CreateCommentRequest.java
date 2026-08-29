package com.marat.taskmanager.securetaskmanager.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment text must not be blank")
    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    private String text;
}