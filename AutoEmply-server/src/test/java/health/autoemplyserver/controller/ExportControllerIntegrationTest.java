package health.autoemplyserver.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportsZipForValidLayoutSpec() throws Exception {
        String payload = """
            {
              "formName": "Form_QRDemo",
              "layoutSpec": {
                "items": [
                  {
                    "type": "Text",
                    "left": 10,
                    "top": 10,
                    "width": 120,
                    "height": 20,
                    "caption": "Demo"
                  }
                ]
              }
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"Form_QRDemo.zip\""))
            .andReturn();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("Form_QRDemo.dfm");
            assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("Form_QRDemo.pas");
            assertThat(zipInputStream.getNextEntry()).isNull();
        }
    }

    @Test
    void returnsDetailedBadRequestForInvalidLayoutSpec() throws Exception {
        String payload = """
            {
              "formName": "___",
              "layoutSpec": {
                "items": []
              }
            }
            """;

        mockMvc.perform(post("/api/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"))
            .andExpect(jsonPath("$.details[0]").value("formName must include at least one non-underscore character."))
            .andExpect(jsonPath("$.details[1]").value("layoutSpec.items must contain at least one item."));
    }
}
