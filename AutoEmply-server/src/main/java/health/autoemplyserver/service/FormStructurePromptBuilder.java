package health.autoemplyserver.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class FormStructurePromptBuilder {

    private static final String SYSTEM_PROMPT_RESOURCE = "/prompts/form-structure-system.txt";

    /** Loaded once at class initialization; the text is maintained in the classpath resource. */
    private static final String SYSTEM_PROMPT = loadSystemPrompt();

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(String formName) {
        return "formName=" + formName + ". Analyze the uploaded form image and extract its complete logical structure. "
            + "Include every visible text element. Use the emit_form_structure tool to return the structure. "
            + "Pay special attention to table column proportions, merged cells (colSpan), and header rows with background colors.";
    }

    private static String loadSystemPrompt() {
        try (InputStream stream = FormStructurePromptBuilder.class.getResourceAsStream(SYSTEM_PROMPT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath resource: " + SYSTEM_PROMPT_RESOURCE);
            }
            // Normalize CRLF to LF so the prompt stays byte-identical to the original Java
            // text block even if the resource is checked out with Windows line endings.
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load classpath resource: " + SYSTEM_PROMPT_RESOURCE, exception);
        }
    }
}
