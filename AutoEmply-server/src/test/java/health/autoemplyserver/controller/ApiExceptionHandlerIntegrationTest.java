package health.autoemplyserver.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import health.autoemplyserver.application.image.ImageGenerationApplicationService;
import health.autoemplyserver.support.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageGenerationApplicationService imageGenerationApplicationService;

    @Test
    void returnsValidationErrorsFromBeanValidation() throws Exception {
        mockMvc.perform(post("/api/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "formName": " ",
                      "layoutSpec": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details[0]").exists())
            .andExpect(jsonPath("$.details[1]").exists());
    }

    @Test
    void returnsUnsupportedMediaTypeError() throws Exception {
        mockMvc.perform(post("/api/export")
                .contentType(MediaType.TEXT_PLAIN)
                .content("not-json"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void mapsExternalServiceFailuresTo502() throws Exception {
        when(imageGenerationApplicationService.generateLayout(anyString(), any(), any(), any()))
            .thenThrow(new ExternalServiceException("Claude overloaded"));

        mockMvc.perform(multipart("/api/generate-json")
                .file(new MockMultipartFile("image", "sample.png", "image/png", new byte[] {1, 2, 3}))
                .param("formName", "Form_QRFail"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").value("Claude overloaded"));
    }
}
