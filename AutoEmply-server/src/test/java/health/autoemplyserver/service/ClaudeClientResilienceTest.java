package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import health.autoemplyserver.config.AiProperties;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.ai.ClaudePayloadFactory;
import health.autoemplyserver.service.ai.ClaudeResponseExtractor;
import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClaudeClientResilienceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesWhenClaudeReturnsEmptyObjectUntilNonEmptyLayoutAppears() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/messages", exchange -> {
            int attempt = requestCount.incrementAndGet();
            if (attempt == 1) {
                writeJson(exchange, 200, """
                    {"model":"claude-test","content":[{"type":"tool_use","input":{}}]}
                    """);
                return;
            }

            writeJson(exchange, 200, """
                {"model":"claude-test","content":[{"type":"tool_use","input":{"items":[{"type":"Text","left":10,"top":10,"width":120,"height":20,"caption":"OK"}]}}]}
                """);
        });
        server.start();

        ClaudeClient client = createClient(server);
        LayoutSpec layoutSpec = client.generateLayoutSpec("Form_QRRetry", "image/png", "ZmFrZQ==", preset());

        assertThat(requestCount.get()).isEqualTo(2);
        assertThat(layoutSpec.getItems()).hasSize(1);
        assertThat(layoutSpec.getItems().getFirst().getCaption()).isEqualTo("OK");
    }

    @Test
    void retriesOnTransientStatusAndRecoversWrappedJsonText() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/messages", exchange -> {
            int attempt = requestCount.incrementAndGet();
            if (attempt == 1) {
                writeJson(exchange, 529, """
                    {"error":{"message":"Overloaded"}}
                    """);
                return;
            }

            writeJson(exchange, 200, """
                {"model":"claude-test","content":[{"type":"text","text":"here is the json\\n{\\"items\\":[{\\"type\\":\\"Text\\",\\"left\\":5,\\"top\\":5,\\"width\\":80,\\"height\\":20,\\"caption\\":\\"Recovered\\"}]}\\nthanks"}]}
                """);
        });
        server.start();

        ClaudeClient client = createClient(server);
        LayoutSpec layoutSpec = client.generateLayoutSpec("Form_QRWrapped", "image/png", "ZmFrZQ==", preset());

        assertThat(requestCount.get()).isEqualTo(2);
        assertThat(layoutSpec.getItems()).hasSize(1);
        assertThat(layoutSpec.getItems().getFirst().getCaption()).isEqualTo("Recovered");
    }

    private ClaudeClient createClient(HttpServer server) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setApiKey("test-key");
        aiProperties.setApiUrl("http://localhost:" + server.getAddress().getPort() + "/v1/messages");
        aiProperties.setMaxRetryAttempts(2);
        aiProperties.setRequestTimeoutSeconds(5);

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

        ObjectMapper objectMapper = new ObjectMapper();
        return new ClaudeClient(
            objectMapper,
            new ClaudeToolSchemas(),
            new LayoutSpecValidator(),
            new FormStructureValidator(),
            new FormStructurePromptBuilder(),
            new ClaudePayloadFactory(),
            new ClaudeResponseExtractor(),
            new AiModelState(),
            aiProperties,
            httpClient
        );
    }

    private ResolvedPromptPreset preset() {
        return new ResolvedPromptPreset(
            java.util.UUID.randomUUID(),
            "default",
            "system",
            "Return JSON for {{formName}}",
            null,
            java.util.List.of(),
            null,
            "claude-test",
            BigDecimal.ZERO,
            2048
        );
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
