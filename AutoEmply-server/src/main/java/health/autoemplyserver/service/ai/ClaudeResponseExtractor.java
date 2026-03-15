package health.autoemplyserver.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class ClaudeResponseExtractor {

    public String extractModelOutput(JsonNode root) {
        if (root == null || !root.has("content") || !root.get("content").isArray()) {
            return null;
        }
        for (JsonNode block : root.get("content")) {
            if ("tool_use".equalsIgnoreCase(block.path("type").asText()) && block.has("input")) {
                return block.get("input").toString();
            }
            if ("text".equalsIgnoreCase(block.path("type").asText()) && block.has("text")) {
                return block.get("text").asText();
            }
        }
        return null;
    }

    public String extractErrorMessage(JsonNode root) {
        if (root != null && root.has("error") && root.get("error").has("message")) {
            return root.get("error").get("message").asText();
        }
        return "Unknown Claude error.";
    }

    public void normalizeStructureAliases(health.autoemplyserver.model.FormStructure structure) {
        if (structure == null || structure.getSections() == null) {
            return;
        }
        structure.getSections().forEach(section -> {
            if (section.getTable() != null && section.getTable().getColumns() != null) {
                section.getTable().getColumns().forEach(column -> {
                    if (column.getHeader() == null) {
                        column.setHeader("");
                    }
                });
            }
        });
    }
}
