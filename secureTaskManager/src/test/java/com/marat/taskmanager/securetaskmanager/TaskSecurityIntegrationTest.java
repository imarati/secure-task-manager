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
}