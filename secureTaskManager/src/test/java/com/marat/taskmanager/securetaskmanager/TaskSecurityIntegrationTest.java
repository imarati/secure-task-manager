package com.marat.taskmanager.securetaskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marat.taskmanager.securetaskmanager.dto.auth.LoginRequest;
import com.marat.taskmanager.securetaskmanager.dto.auth.RegisterRequest;
import com.marat.taskmanager.securetaskmanager.dto.task.CreateTaskRequest;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskPriority;
import com.marat.taskmanager.securetaskmanager.entity.enums.TaskStatus;
import com.marat.taskmanager.securetaskmanager.repository.CommentRepository;
import com.marat.taskmanager.securetaskmanager.repository.TaskRepository;
import com.marat.taskmanager.securetaskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_shouldReturnToken() throws Exception {
        RegisterRequest request = registerRequest(
                "user-a@example.com",
                "password123",
                "User A"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withCorrectPassword_shouldReturnToken() throws Exception {
        register("user-a@example.com", "password123", "User A");

        LoginRequest request = new LoginRequest();
        request.setEmail("user-a@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void createTask_withoutJwt_shouldBeUnauthorized() throws Exception {
        CreateTaskRequest request = createTaskRequest(
                "Task without token",
                "This request must be denied",
                TaskPriority.LOW
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userA_canCreateAndReadOwnTask() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");

        Long taskId = createTask(tokenA, "Task of User A");

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Task of User A"))
                .andExpect(jsonPath("$.status").value(TaskStatus.TODO.name()))
                .andExpect(jsonPath("$.priority").value(TaskPriority.LOW.name()));
    }

    @Test
    void userB_cannotReadUpdateOrDeleteTaskOfUserA() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        String tokenB = register("user-b@example.com", "password123", "User B");

        Long taskId = createTask(tokenA, "Private task of User A");

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        String updateJson = """
                {
                  "title": "Hacked title",
                  "description": "User B must not update this",
                  "status": "DONE",
                  "priority": "HIGH"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_withBlankTitle_shouldReturnBadRequest() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");

        String invalidTaskJson = """
                {
                  "title": "",
                  "description": "Task with invalid title",
                  "priority": "LOW"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTaskJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title")
                        .value("Title must not be blank"));
    }

    @Test
    void login_withWrongPassword_shouldReturnUnauthorized() throws Exception {
        register("user-a@example.com", "password123", "User A");

        LoginRequest request = new LoginRequest();
        request.setEmail("user-a@example.com");
        request.setPassword("wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withExistingEmail_shouldReturnConflict() throws Exception {
        register("user-a@example.com", "password123", "User A");

        RegisterRequest duplicateRequest = registerRequest(
                "user-a@example.com",
                "another-password",
                "User A duplicate"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void owner_canUpdateOwnTask() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        Long taskId = createTask(tokenA, "Original title");

        String updateJson = """
            {
              "title": "Updated title",
              "description": "Updated description",
              "status": "IN_PROGRESS",
              "priority": "HIGH"
            }
            """;

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void owner_canDeleteOwnTask() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        Long taskId = createTask(tokenA, "Task to delete");

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void owner_canCreateCommentForOwnTask() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        Long taskId = createTask(tokenA, "Task with comment");

        String commentJson = """
            {
              "text": "First comment"
            }
            """;

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.text").value("First comment"))
                .andExpect(jsonPath("$.taskId").value(taskId));
    }

    @Test
    void owner_canGetCommentsForOwnTask() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        Long taskId = createTask(tokenA, "Task with comments");

        createComment(tokenA, taskId, "First comment");
        createComment(tokenA, taskId, "Second comment");

        mockMvc.perform(get("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("First comment"))
                .andExpect(jsonPath("$[1].text").value("Second comment"));
    }

    @Test
    void userB_cannotCreateCommentForTaskOfUserA() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        String tokenB = register("user-b@example.com", "password123", "User B");

        Long taskId = createTask(tokenA, "Private task");

        String commentJson = """
            {
              "text": "User B must not comment here"
            }
            """;

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void userB_cannotReadCommentsForTaskOfUserA() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        String tokenB = register("user-b@example.com", "password123", "User B");

        Long taskId = createTask(tokenA, "Private task");
        createComment(tokenA, taskId, "Private comment");

        mockMvc.perform(get("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void createComment_withoutJwt_shouldBeUnauthorized() throws Exception {
        String commentJson = """
            {
              "text": "Anonymous comment"
            }
            """;

        mockMvc.perform(post("/api/tasks/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commentAuthor_canDeleteOwnComment() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        Long taskId = createTask(tokenA, "Task with a comment");
        Long commentId = createComment(tokenA, taskId, "Comment to delete");

        mockMvc.perform(
                        delete("/api/tasks/" + taskId + "/comments/" + commentId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userB_cannotDeleteCommentOfUserA() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        String tokenB = register("user-b@example.com", "password123", "User B");

        Long taskId = createTask(tokenA, "Private task");
        Long commentId = createComment(tokenA, taskId, "Private comment");

        mockMvc.perform(
                        delete("/api/tasks/" + taskId + "/comments/" + commentId)
                                .header("Authorization", "Bearer " + tokenB)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_withCommentFromAnotherTask_shouldReturnNotFound() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");

        Long firstTaskId = createTask(tokenA, "First task");
        Long secondTaskId = createTask(tokenA, "Second task");

        Long commentId = createComment(tokenA, firstTaskId, "Comment on first task");

        mockMvc.perform(
                        delete("/api/tasks/" + secondTaskId + "/comments/" + commentId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void userA_canGetOnlyOwnTasksWithPagination() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");
        String tokenB = register("user-b@example.com", "password123", "User B");

        createTask(tokenA, "A task 1");
        createTask(tokenA, "A task 2");
        createTask(tokenA, "A task 3");
        createTask(tokenB, "B private task");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].title").value("A task 1"))
                .andExpect(jsonPath("$.content[1].title").value("A task 2"));
    }

    @Test
    void userA_canFilterOwnTasksByStatusAndPriority() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");

        Long taskId = createTask(tokenA, "Important task");

        String updateJson = """
            {
              "title": "Important task",
              "description": "Must be completed",
              "status": "IN_PROGRESS",
              "priority": "HIGH"
            }
            """;

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        createTask(tokenA, "Ordinary task");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.content[0].title").value("Important task"));
    }

    @Test
    void userA_getsTasksSortedByCreatedAtDescByDefault() throws Exception {
        String tokenA = register("user-a@example.com", "password123", "User A");

        createTask(tokenA, "First task");
        createTask(tokenA, "Second task");
        createTask(tokenA, "Third task");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Third task"))
                .andExpect(jsonPath("$.content[1].title").value("Second task"))
                .andExpect(jsonPath("$.content[2].title").value("First task"));
    }

    private RegisterRequest registerRequest(
            String email,
            String password,
            String displayName
    ) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setDisplayName(displayName);

        return request;
    }

    private String register(
            String email,
            String password,
            String displayName
    ) throws Exception {
        RegisterRequest request = registerRequest(email, password, displayName);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String token = json.get("token").asText();

        assertThat(token).isNotBlank();

        return token;
    }

    private Long createTask(String jwtToken, String title) throws Exception {
        CreateTaskRequest request = createTaskRequest(
                title,
                "Created in integration test",
                TaskPriority.LOW
        );

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return json.get("id").asLong();
    }

    private CreateTaskRequest createTaskRequest(
            String title,
            String description,
            TaskPriority priority
    ) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setPriority(priority);

        return request;
    }

    private Long createComment(
            String jwtToken,
            Long taskId,
            String text
    ) throws Exception {
        String commentJson = objectMapper.writeValueAsString(
                java.util.Map.of("text", text)
        );

        MvcResult result = mockMvc.perform(
                        post("/api/tasks/" + taskId + "/comments")
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentJson)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return json.get("id").asLong();
    }
}