package health.autoemplyserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PromptsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsUpdatesListsAndDeletesPromptPreset() throws Exception {
        String createPayload = """
            {
              "name": "Default Prompt",
              "systemPrompt": "system",
              "userPromptTemplate": "user {{formName}}",
              "styleRulesJson": "{\\"tone\\":\\"strict\\"}",
              "model": "claude-test",
              "temperature": 0.2,
              "maxTokens": 4096,
              "isActive": true
            }
            """;

        String responseBody = mockMvc.perform(post("/api/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Default Prompt"))
            .andExpect(jsonPath("$.systemPrompt").value("system"))
            .andReturn()
            .getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(responseBody);
        String id = created.get("id").asText();

        mockMvc.perform(get("/api/prompts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id));

        String updatePayload = """
            {
              "name": "Updated Prompt",
              "systemPrompt": "system v2",
              "userPromptTemplate": "user v2",
              "styleRulesJson": "{\\"tone\\":\\"calm\\"}",
              "model": "claude-test",
              "temperature": 0.3,
              "maxTokens": 8192,
              "isActive": false
            }
            """;

        mockMvc.perform(put("/api/prompts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Prompt"))
            .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(delete("/api/prompts/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/prompts/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Preset not found."));
    }

    @Test
    void returnsBadRequestForInvalidPromptJson() throws Exception {
        String payload = """
            {
              "name": "Broken Prompt",
              "systemPrompt": "system",
              "userPromptTemplate": "user",
              "styleRulesJson": "{broken}",
              "isActive": true
            }
            """;

        mockMvc.perform(post("/api/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("styleRulesJson is not valid JSON."));
    }
}
