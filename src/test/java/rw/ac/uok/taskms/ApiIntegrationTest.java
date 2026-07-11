package rw.ac.uok.taskms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void loginReturnsTokenAndMeWorks() throws Exception {
        String token = login("admin@uok.ac.rw", "admin123");
        assertThat(token).isNotBlank();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginFailsWithBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@uok.ac.rw\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void officerCannotAccessAdminEndpoints() throws Exception {
        String token = login("alice@uok.ac.rw", "officer123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        String token = login("admin@uok.ac.rw", "admin123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void createTaskProducesPredictionWithConfidenceInterval() throws Exception {
        String token = login("manager@uok.ac.rw", "manager123");

        // Grab a task type and an assignee id via the API / seed.
        MvcResult typesRes = mvc.perform(get("/api/task-types").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        long typeId = mapper.readTree(typesRes.getResponse().getContentAsString()).get(0).get("id").asLong();

        MvcResult tasksRes = mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        long assigneeId = mapper.readTree(tasksRes.getResponse().getContentAsString()).get(0).get("assigneeId").asLong();

        String body = mapper.writeValueAsString(java.util.Map.of(
                "title", "Integration test task",
                "taskTypeId", typeId,
                "assigneeId", assigneeId,
                "complexity", "MEDIUM",
                "estimatedDurationDays", 3.0,
                "dueDate", LocalDate.now().plusDays(5).toString()));

        MvcResult created = mvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedDurationDays").isNumber())
                .andReturn();

        JsonNode node = mapper.readTree(created.getResponse().getContentAsString());
        assertThat(node.get("predictedLowerDays").asDouble())
                .isLessThanOrEqualTo(node.get("predictedDurationDays").asDouble());
        assertThat(node.get("predictedUpperDays").asDouble())
                .isGreaterThanOrEqualTo(node.get("predictedDurationDays").asDouble());
    }
}
