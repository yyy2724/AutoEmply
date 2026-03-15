package health.autoemplyserver;

import health.autoemplyserver.service.AiModelState;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AutoEmplyServerApplication {

    public static void main(String[] args) {
        applyDotNetEnvironmentCompatibility();
        SpringApplication.run(AutoEmplyServerApplication.class, args);
    }

    @Bean
    AiModelState aiModelState() {
        return new AiModelState();
    }

    private static void applyDotNetEnvironmentCompatibility() {
        bridgeConnectionString();
        bridgeAnthropicSettings();
    }

    private static void bridgeConnectionString() {
        String connectionString = firstNonBlank(
            System.getenv("ConnectionStrings__Default"),
            System.getenv("CONNECTIONSTRINGS__DEFAULT")
        );
        if (isBlank(connectionString)) {
            return;
        }

        Map<String, String> values = parseConnectionString(connectionString);
        String host = firstMappedValue(values, "host", "server", "data source", "datasource");
        String port = firstMappedValue(values, "port");
        String database = firstMappedValue(values, "database", "initial catalog");
        String username = firstMappedValue(values, "username", "user id", "user", "userid", "uid");
        String password = firstMappedValue(values, "password", "pwd");

        if (!isBlank(host) && !isBlank(database) && isBlank(System.getProperty("spring.datasource.url"))) {
            String resolvedPort = isBlank(port) ? "5432" : port.trim();
            System.setProperty(
                "spring.datasource.url",
                "jdbc:postgresql://" + host.trim() + ":" + resolvedPort + "/" + database.trim()
            );
        }
        if (!isBlank(username) && isBlank(System.getProperty("spring.datasource.username"))) {
            System.setProperty("spring.datasource.username", username.trim());
        }
        if (!isBlank(password) && isBlank(System.getProperty("spring.datasource.password"))) {
            System.setProperty("spring.datasource.password", password);
        }
    }

    private static void bridgeAnthropicSettings() {
        copyEnvToSystemProperty("ANTHROPIC_API_KEY", "anthropic.api-key");
        copyEnvToSystemProperty("Anthropic__ApiUrl", "app.ai.api-url");
        copyEnvToSystemProperty("ANTHROPIC__APIURL", "app.ai.api-url");
        copyEnvToSystemProperty("Anthropic__Model", "app.ai.model");
        copyEnvToSystemProperty("ANTHROPIC__MODEL", "app.ai.model");
        copyEnvToSystemProperty("Anthropic__RequestTimeoutSeconds", "app.ai.request-timeout-seconds");
        copyEnvToSystemProperty("ANTHROPIC__REQUESTTIMEOUTSECONDS", "app.ai.request-timeout-seconds");
        copyEnvToSystemProperty("Anthropic__MaxRetryAttempts", "app.ai.max-retry-attempts");
        copyEnvToSystemProperty("ANTHROPIC__MAXRETRYATTEMPTS", "app.ai.max-retry-attempts");
    }

    private static void copyEnvToSystemProperty(String envName, String propertyName) {
        if (!isBlank(System.getProperty(propertyName))) {
            return;
        }
        String value = System.getenv(envName);
        if (!isBlank(value)) {
            System.setProperty(propertyName, value.trim());
        }
    }

    private static Map<String, String> parseConnectionString(String connectionString) {
        Map<String, String> values = new HashMap<>();
        for (String part : connectionString.split(";")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(separatorIndex + 1).trim();
            values.put(key, value);
        }
        return values;
    }

    private static String firstMappedValue(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
