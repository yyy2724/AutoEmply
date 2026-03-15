package health.autoemplyserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import health.autoemplyserver.config.AiProperties;
import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.ai.ClaudePayloadFactory;
import health.autoemplyserver.service.ai.ClaudeResponseExtractor;
import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.ExternalServiceException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final ObjectMapper objectMapper;
    private final ClaudeToolSchemas claudeToolSchemas;
    private final LayoutSpecValidator layoutSpecValidator;
    private final FormStructureValidator formStructureValidator;
    private final FormStructurePromptBuilder formStructurePromptBuilder;
    private final ClaudePayloadFactory claudePayloadFactory;
    private final ClaudeResponseExtractor claudeResponseExtractor;
    private final AiModelState aiModelState;
    private final AiProperties aiProperties;
    private final HttpClient httpClient;

    @Autowired
    public ClaudeClient(
        ObjectMapper objectMapper,
        ClaudeToolSchemas claudeToolSchemas,
        LayoutSpecValidator layoutSpecValidator,
        FormStructureValidator formStructureValidator,
        FormStructurePromptBuilder formStructurePromptBuilder,
        ClaudePayloadFactory claudePayloadFactory,
        ClaudeResponseExtractor claudeResponseExtractor,
        AiModelState aiModelState,
        AiProperties aiProperties
    ) {
        this(
            objectMapper,
            claudeToolSchemas,
            layoutSpecValidator,
            formStructureValidator,
            formStructurePromptBuilder,
            claudePayloadFactory,
            claudeResponseExtractor,
            aiModelState,
            aiProperties,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(10, aiProperties.getRequestTimeoutSeconds())))
                .build()
        );
    }

    ClaudeClient(
        ObjectMapper objectMapper,
        ClaudeToolSchemas claudeToolSchemas,
        LayoutSpecValidator layoutSpecValidator,
        FormStructureValidator formStructureValidator,
        FormStructurePromptBuilder formStructurePromptBuilder,
        ClaudePayloadFactory claudePayloadFactory,
        ClaudeResponseExtractor claudeResponseExtractor,
        AiModelState aiModelState,
        AiProperties aiProperties,
        HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.claudeToolSchemas = claudeToolSchemas;
        this.layoutSpecValidator = layoutSpecValidator;
        this.formStructureValidator = formStructureValidator;
        this.formStructurePromptBuilder = formStructurePromptBuilder;
        this.claudePayloadFactory = claudePayloadFactory;
        this.claudeResponseExtractor = claudeResponseExtractor;
        this.aiModelState = aiModelState;
        this.aiProperties = aiProperties;
        this.httpClient = httpClient;
    }

    public LayoutSpec generateLayoutSpec(String formName, String mediaType, String base64Data, ResolvedPromptPreset preset) {
        return generateLayoutSpec(formName, mediaType, base64Data, preset, false, 0);
    }

    private LayoutSpec generateLayoutSpec(String formName, String mediaType, String base64Data, ResolvedPromptPreset preset, boolean forceNonEmptyItems, int emptyRetryLevel) {
        ParseOutcome<LayoutSpec> outcome = callClaudeWithRetries(
            formName,
            mediaType,
            base64Data,
            preset,
            preset.systemPrompt(),
            claudeToolSchemas.buildLayoutSpecTool(),
            "emit_layout_spec",
            appendLayoutGuidance(claudePayloadFactory.renderUserPrompt(preset.userPromptTemplate(), formName), forceNonEmptyItems, null),
            (rawText, attempt) -> {
                if (isEmptyObjectPayload(rawText) && emptyRetryLevel < 2) {
                    return ParseOutcome.recursiveRetry();
                }

                ParseResult<LayoutSpec> parseResult = deserializeToolPayload(rawText, LayoutSpec.class);
                if (!parseResult.success()) {
                    return ParseOutcome.retry(parseResult.message());
                }

                List<String> errors = layoutSpecValidator.validate(formName, parseResult.value());
                if (errors.isEmpty()) {
                    return ParseOutcome.success(parseResult.value());
                }

                boolean itemsEmpty = errors.stream()
                    .anyMatch(error -> error.contains("layoutSpec.items must contain at least one item."));
                if (!forceNonEmptyItems && itemsEmpty && emptyRetryLevel < 2) {
                    return ParseOutcome.recursiveRetry();
                }

                return ParseOutcome.retry(String.join(" | ", errors.stream().limit(5).toList()));
            }
        );

        if (outcome.needsRecursiveRetry()) {
            log.warn("Claude returned empty or items-empty layout payload. Escalating prompt constraints. level={}", emptyRetryLevel + 1);
            return generateLayoutSpec(formName, mediaType, base64Data, preset, true, emptyRetryLevel + 1);
        }

        if (outcome.success()) {
            return outcome.value();
        }

        throw toRuntimeException(outcome);
    }

    public FormStructure generateFormStructure(String formName, String mediaType, String base64Data, ResolvedPromptPreset preset) {
        ParseOutcome<FormStructure> outcome = callClaudeWithRetries(
            formName,
            mediaType,
            base64Data,
            preset,
            formStructurePromptBuilder.buildSystemPrompt(),
            claudeToolSchemas.buildFormStructureTool(),
            "emit_form_structure",
            formStructurePromptBuilder.buildUserPrompt(formName),
            (rawText, attempt) -> {
                ParseResult<FormStructure> parseResult = deserializeToolPayload(rawText, FormStructure.class);
                if (!parseResult.success()) {
                    return ParseOutcome.retry(parseResult.message());
                }

                claudeResponseExtractor.normalizeStructureAliases(parseResult.value());
                List<String> errors = formStructureValidator.validate(parseResult.value());
                if (errors.isEmpty()) {
                    return ParseOutcome.success(parseResult.value());
                }

                return ParseOutcome.retry(String.join(" | ", errors.stream().limit(5).toList()));
            }
        );

        if (outcome.success()) {
            return outcome.value();
        }

        throw toRuntimeException(outcome);
    }

    private <T> ParseOutcome<T> callClaudeWithRetries(
        String formName,
        String mediaType,
        String base64Data,
        ResolvedPromptPreset preset,
        String systemPrompt,
        Map<String, Object> toolSchema,
        String toolName,
        String baseUserPrompt,
        BiFunction<String, Integer, ParseOutcome<T>> parseAndValidate
    ) {
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return ParseOutcome.failure(400, "ANTHROPIC_API_KEY is required.", List.of());
        }

        int maxRetries = Math.max(1, aiProperties.getMaxRetryAttempts());
        ParseOutcome<T> lastFailure = null;
        String retryGuidance = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String userPrompt = appendRetryGuidance(baseUserPrompt, retryGuidance, attempt);
            Map<String, Object> payload = claudePayloadFactory.build(
                preset,
                systemPrompt,
                toolSchema,
                toolName,
                userPrompt,
                mediaType,
                base64Data
            );

            HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getApiUrl()))
                    .timeout(Duration.ofSeconds(Math.max(30, aiProperties.getRequestTimeoutSeconds())))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize Claude payload.", exception);
            }

            try {
                log.info("Claude request attempt {}/{} model={} mediaType={} formName={}", attempt, maxRetries, preset.model(), mediaType, formName);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = safeReadTree(response.body());
                String requestId = extractRequestId(response, root);
                updateResponseModel(root, preset.model());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String message = claudeResponseExtractor.extractErrorMessage(root);
                    log.warn("Claude API failure attempt={}/{} status={} requestId={} message={}", attempt, maxRetries, response.statusCode(), requestId, message);

                    lastFailure = ParseOutcome.failure(
                        response.statusCode(),
                        "Claude API request failed.",
                        List.of("ClaudeStatus=" + response.statusCode(), message)
                    );

                    if (attempt < maxRetries && isTransient(response.statusCode())) {
                        sleepBackoff(attempt);
                        continue;
                    }
                    return lastFailure;
                }

                String rawText = claudeResponseExtractor.extractModelOutput(root);
                if (rawText == null || rawText.isBlank()) {
                    log.warn("Claude response had no usable tool output attempt={}/{} requestId={}", attempt, maxRetries, requestId);
                    lastFailure = ParseOutcome.failure(400, "Claude response did not contain tool output.", List.of());
                    if (attempt < maxRetries) {
                        retryGuidance = "Return strict JSON only via the requested tool. Do not answer in prose.";
                        sleepBackoff(attempt);
                        continue;
                    }
                    return lastFailure;
                }

                log.info("Claude response success attempt={}/{} requestId={} preview={}", attempt, maxRetries, requestId, truncate(rawText, 600));

                ParseOutcome<T> outcome = parseAndValidate.apply(rawText, attempt);
                if (outcome.needsRecursiveRetry()) {
                    return outcome;
                }
                if (outcome.success()) {
                    return outcome;
                }

                lastFailure = ParseOutcome.failure(
                    400,
                    outcome.error() == null || outcome.error().isBlank() ? "Failed to parse Claude JSON output." : outcome.error(),
                    outcome.details()
                );

                if (attempt < maxRetries) {
                    retryGuidance = outcome.retryGuidance();
                    log.warn("Claude parse/validation failed attempt={}/{} guidance={}", attempt, maxRetries, retryGuidance);
                    sleepBackoff(attempt);
                    continue;
                }

                return lastFailure;
            } catch (IOException exception) {
                log.warn("Claude network failure attempt={}/{}", attempt, maxRetries, exception);
                lastFailure = ParseOutcome.failure(503, "Claude network request failed.", List.of(exception.getMessage()));
                if (attempt < maxRetries) {
                    try {
                        sleepBackoff(attempt);
                        continue;
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return ParseOutcome.failure(504, "Claude request interrupted.", List.of(interruptedException.getMessage()));
                    }
                }
                return lastFailure;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return ParseOutcome.failure(504, "Claude request interrupted.", List.of(exception.getMessage()));
            }
        }

        return lastFailure == null
            ? ParseOutcome.failure(502, "Claude request failed.", List.of())
            : lastFailure;
    }

    private RuntimeException toRuntimeException(ParseOutcome<?> outcome) {
        if (outcome.statusCode() >= 500) {
            return new ExternalServiceException(outcome.error() == null ? "Claude request failed." : outcome.error());
        }
        return new BadRequestException(
            outcome.error() == null ? "Failed to parse Claude JSON output." : outcome.error(),
            outcome.details() == null ? List.of() : outcome.details()
        );
    }

    private <T> ParseResult<T> deserializeToolPayload(String rawText, Class<T> type) {
        try {
            return ParseResult.success(objectMapper.readValue(rawText, type));
        } catch (JsonProcessingException exception) {
            int first = rawText.indexOf('{');
            int last = rawText.lastIndexOf('}');
            if (first >= 0 && last > first) {
                try {
                    return ParseResult.success(objectMapper.readValue(rawText.substring(first, last + 1), type));
                } catch (JsonProcessingException ignored) {
                }
            }
            return ParseResult.failure("JSON schema/type mismatch: " + exception.getOriginalMessage());
        }
    }

    private JsonNode safeReadTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse Claude response body as JSON.");
            return null;
        }
    }

    private void updateResponseModel(JsonNode root, String fallbackModel) {
        if (root != null && root.hasNonNull("model")) {
            aiModelState.update(root.get("model").asText());
            return;
        }
        aiModelState.update(fallbackModel);
    }

    private String extractRequestId(HttpResponse<String> response, JsonNode root) {
        String requestId = response.headers().firstValue("request-id").orElse(null);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        requestId = response.headers().firstValue("x-request-id").orElse(null);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        if (root != null && root.hasNonNull("request_id")) {
            return root.get("request_id").asText();
        }
        return "unknown";
    }

    private boolean isTransient(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode == 529;
    }

    private boolean isEmptyObjectPayload(String rawText) {
        return "{}".equals(rawText == null ? "" : rawText.trim());
    }

    private String appendLayoutGuidance(String prompt, boolean forceNonEmptyItems, String retryGuidance) {
        StringBuilder builder = new StringBuilder(prompt == null ? "" : prompt.trim());
        if (forceNonEmptyItems) {
            builder.append("\n\nHard requirement: return a non-empty items array. Do not return {} or {\"items\":[]}.");
        }
        if (retryGuidance != null && !retryGuidance.isBlank()) {
            builder.append("\n\nRetry requirement: ").append(retryGuidance);
        }
        return builder.toString().trim();
    }

    private String appendRetryGuidance(String prompt, String retryGuidance, int attempt) {
        if (attempt <= 1 || retryGuidance == null || retryGuidance.isBlank()) {
            return prompt;
        }
        return (prompt + "\n\nPrevious output was invalid. Fix this issue and return strict JSON only: " + retryGuidance).trim();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private void sleepBackoff(int attempt) throws InterruptedException {
        Thread.sleep((long) Math.pow(2, attempt - 1) * 1000L);
    }

    private record ParseResult<T>(boolean success, T value, String message) {
        static <T> ParseResult<T> success(T value) {
            return new ParseResult<>(true, value, null);
        }

        static <T> ParseResult<T> failure(String message) {
            return new ParseResult<>(false, null, message);
        }
    }

    private record ParseOutcome<T>(
        boolean success,
        int statusCode,
        String error,
        List<String> details,
        T value,
        String retryGuidance,
        boolean needsRecursiveRetry
    ) {
        static <T> ParseOutcome<T> success(T value) {
            return new ParseOutcome<>(true, 200, null, List.of(), value, null, false);
        }

        static <T> ParseOutcome<T> retry(String retryGuidance) {
            return new ParseOutcome<>(false, 0, null, List.of(), null, retryGuidance, false);
        }

        static <T> ParseOutcome<T> recursiveRetry() {
            return new ParseOutcome<>(false, 0, null, List.of(), null, null, true);
        }

        static <T> ParseOutcome<T> failure(int statusCode, String error, List<String> details) {
            return new ParseOutcome<>(false, statusCode, error, details == null ? List.of() : List.copyOf(details), null, null, false);
        }
    }
}
