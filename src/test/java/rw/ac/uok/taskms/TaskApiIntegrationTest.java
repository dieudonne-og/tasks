package rw.ac.uok.taskms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("email", email);
            put("password", "password123");
        }});
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void officerCanViewBoard() throws Exception {
        String token = login("officer1@uok.ac.rw");
        String json = mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
    }

    @Test
    void officerCannotCreateTaskType() throws Exception {
        String token = login("officer1@uok.ac.rw");
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", "Illegal type");
            put("description", "should be blocked");
        }});
        mockMvc.perform(post("/api/task-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateTaskType() throws Exception {
        String token = login("admin@uok.ac.rw");
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", "Special onboarding review");
            put("description", "created in a test");
        }});
        mockMvc.perform(post("/api/task-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void predictionsAreAppliedToSeededTasks() throws Exception {
        String token = login("manager@uok.ac.rw");
        String json = mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = objectMapper.readTree(json);
        assertThat(arr).isNotEmpty();
        // Every seeded open task should carry a prediction with a confidence interval.
        boolean anyPredicted = false;
        for (JsonNode t : arr) {
            if (!t.get("predictedDurationDays").isNull()) {
                anyPredicted = true;
                assertThat(t.get("predictedLowerDays").asDouble())
                        .isLessThanOrEqualTo(t.get("predictedDurationDays").asDouble());
                assertThat(t.get("predictedUpperDays").asDouble())
                        .isGreaterThanOrEqualTo(t.get("predictedDurationDays").asDouble());
            }
        }
        assertThat(anyPredicted).isTrue();
    }
}
