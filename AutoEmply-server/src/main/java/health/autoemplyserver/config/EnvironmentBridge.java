package health.autoemplyserver.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bridges .NET-style environment variables to Spring configuration properties.
 *
 * <p>This server replaced a .NET service whose deployment environment still provides
 * configuration the .NET way: an ADO.NET-style connection string
 * ({@code ConnectionStrings__Default}, e.g. {@code "Host=db;Port=5432;Database=app;Username=u;Password=p"})
 * and {@code Anthropic__*} settings. Spring cannot consume these directly, so before the
 * application context starts we parse them and copy the values into the equivalent Spring
 * system properties ({@code spring.datasource.*}, {@code app.ai.*}, {@code anthropic.api-key}).
 * Explicitly set system properties always win; this bridge only fills in blanks.</p>
 */
public final class EnvironmentBridge {

    private EnvironmentBridge() {
    }

    /**
     * Applies all .NET-to-Spring property bridging using the process environment. Must run
     * before {@code SpringApplication.run(...)} so the properties are visible to the environment.
     */
    public static void applyDotNetEnvironmentCompatibility() {
        apply(System.getenv());
    }

    /**
     * Applies all .NET-to-Spring property bridging using the given environment variables.
     * Package-private seam so tests can supply an environment without reflection.
     */
    static void apply(Map<String, String> env) {
        bridgeConnectionString(env);
        bridgeAnthropicSettings(env);
    }

    private static void bridgeConnectionString(Map<String, String> env) {
        String connectionString = firstNonBlank(
            env.get("ConnectionStrings__Default"),
            env.get("CONNECTIONSTRINGS__DEFAULT")
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

    private static void bridgeAnthropicSettings(Map<String, String> env) {
        copyEnvToSystemProperty(env, "anthropic.api-key", "ANTHROPIC_API_KEY");
        copyEnvToSystemProperty(env, "app.ai.api-url", "Anthropic__ApiUrl", "ANTHROPIC__APIURL");
        copyEnvToSystemProperty(env, "app.ai.model", "Anthropic__Model", "ANTHROPIC__MODEL");
        copyEnvToSystemProperty(env, "app.ai.request-timeout-seconds",
            "Anthropic__RequestTimeoutSeconds", "ANTHROPIC__REQUESTTIMEOUTSECONDS");
        copyEnvToSystemProperty(env, "app.ai.max-retry-attempts",
            "Anthropic__MaxRetryAttempts", "ANTHROPIC__MAXRETRYATTEMPTS");
    }

    /** Copies the first non-blank env value (names tried in order) into the property, unless already set. */
    private static void copyEnvToSystemProperty(Map<String, String> env, String propertyName, String... envNames) {
        if (!isBlank(System.getProperty(propertyName))) {
            return;
        }
        for (String envName : envNames) {
            String value = env.get(envName);
            if (!isBlank(value)) {
                System.setProperty(propertyName, value.trim());
                return;
            }
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
