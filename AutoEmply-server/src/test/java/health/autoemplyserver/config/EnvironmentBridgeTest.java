package health.autoemplyserver.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EnvironmentBridge} through its package-private {@code apply(Map)} seam, which
 * lets the environment be supplied directly instead of stubbing {@link System#getenv}. The
 * bridged system properties are saved before and restored after each test so the suite stays
 * hermetic regardless of what the host JVM has set.
 */
class EnvironmentBridgeTest {

    private static final List<String> BRIDGED_PROPERTIES = List.of(
        "spring.datasource.url",
        "spring.datasource.username",
        "spring.datasource.password",
        "anthropic.api-key",
        "app.ai.api-url",
        "app.ai.model",
        "app.ai.request-timeout-seconds",
        "app.ai.max-retry-attempts");

    private final Map<String, String> savedProperties = new HashMap<>();

    @BeforeEach
    void saveAndClearBridgedSystemProperties() {
        for (String key : BRIDGED_PROPERTIES) {
            savedProperties.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
    }

    @AfterEach
    void restoreBridgedSystemProperties() {
        for (String key : BRIDGED_PROPERTIES) {
            String original = savedProperties.get(key);
            if (original == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, original);
            }
        }
    }

    @Test
    void bridgesFullConnectionStringIntoDatasourceProperties() {
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default",
            "Host=db.example.com;Port=5433;Database=app;Username=svc;Password=secret"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://db.example.com:5433/app");
        assertThat(System.getProperty("spring.datasource.username")).isEqualTo("svc");
        assertThat(System.getProperty("spring.datasource.password")).isEqualTo("secret");
    }

    @Test
    void defaultsToPort5432WhenConnectionStringHasNoPort() {
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default", "Host=db.example.com;Database=app"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://db.example.com:5432/app");
    }

    @Test
    void resolvesDotNetAliasKeysForHostDatabaseUsernameAndPassword() {
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default",
            "Server=alias-db;Initial Catalog=alias-cat;User Id=alias-user;Pwd=alias-pw"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://alias-db:5432/alias-cat");
        assertThat(System.getProperty("spring.datasource.username")).isEqualTo("alias-user");
        assertThat(System.getProperty("spring.datasource.password")).isEqualTo("alias-pw");
    }

    @Test
    void preferredKeyWinsOverAliasAndBlankPreferredFallsThroughToAlias() {
        // "host" outranks "server"; a blank "username" falls through to "user id".
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default",
            "Host=primary-db;Server=ignored-db;Database=app;Username= ;User Id=fallback-user"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://primary-db:5432/app");
        assertThat(System.getProperty("spring.datasource.username")).isEqualTo("fallback-user");
    }

    @Test
    void parsingTrimsSegmentsSkipsMalformedOnesAndSplitsOnFirstEqualsOnly() {
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default",
            " Server = my-db ;;noequals;=orphan;Database=app;Pwd=a=b"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://my-db:5432/app");
        assertThat(System.getProperty("spring.datasource.password")).isEqualTo("a=b");
    }

    @Test
    void skipsDatasourceUrlWhenHostOrDatabaseIsMissing() {
        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default", "Host=db.example.com;Username=svc"));

        assertThat(System.getProperty("spring.datasource.url")).isNull();
        assertThat(System.getProperty("spring.datasource.username")).isEqualTo("svc");
    }

    @Test
    void fallsBackToUppercaseConnectionStringEnvVariable() {
        EnvironmentBridge.apply(Map.of(
            "CONNECTIONSTRINGS__DEFAULT", "Host=upper-db;Database=app"));

        assertThat(System.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://upper-db:5432/app");
    }

    @Test
    void bridgesAnthropicSettingsAndTrimsValues() {
        EnvironmentBridge.apply(Map.of(
            "ANTHROPIC_API_KEY", " key-123 ",
            "Anthropic__ApiUrl", "https://api.example.com",
            "ANTHROPIC__MODEL", "claude-opus-4-7",
            "Anthropic__RequestTimeoutSeconds", "90",
            "ANTHROPIC__MAXRETRYATTEMPTS", "5"));

        assertThat(System.getProperty("anthropic.api-key")).isEqualTo("key-123");
        assertThat(System.getProperty("app.ai.api-url")).isEqualTo("https://api.example.com");
        assertThat(System.getProperty("app.ai.model")).isEqualTo("claude-opus-4-7");
        assertThat(System.getProperty("app.ai.request-timeout-seconds")).isEqualTo("90");
        assertThat(System.getProperty("app.ai.max-retry-attempts")).isEqualTo("5");
    }

    @Test
    void applyIsNoOpWhenEnvironmentIsEmpty() {
        EnvironmentBridge.apply(Map.of());

        for (String key : BRIDGED_PROPERTIES) {
            assertThat(System.getProperty(key)).isNull();
        }
    }

    @Test
    void applyNeverOverwritesExplicitlySetSystemProperties() {
        // The bridge only fills blanks, so pre-set properties must survive even when the
        // environment supplies competing values.
        for (String key : BRIDGED_PROPERTIES) {
            System.setProperty(key, "explicit-" + key);
        }

        EnvironmentBridge.apply(Map.of(
            "ConnectionStrings__Default", "Host=env-db;Port=9999;Database=env;Username=env-u;Password=env-p",
            "ANTHROPIC_API_KEY", "env-key",
            "Anthropic__ApiUrl", "https://env.example.com",
            "Anthropic__Model", "env-model",
            "Anthropic__RequestTimeoutSeconds", "1",
            "Anthropic__MaxRetryAttempts", "1"));

        for (String key : BRIDGED_PROPERTIES) {
            assertThat(System.getProperty(key)).isEqualTo("explicit-" + key);
        }
    }

    @Test
    void publicEntryPointDelegatesWithoutDisturbingPresetProperties() {
        // applyDotNetEnvironmentCompatibility() reads the real process environment, so the
        // only portable assertion is the fill-blanks-only contract: explicitly set
        // properties always survive it.
        for (String key : BRIDGED_PROPERTIES) {
            System.setProperty(key, "explicit-" + key);
        }

        EnvironmentBridge.applyDotNetEnvironmentCompatibility();

        for (String key : BRIDGED_PROPERTIES) {
            assertThat(System.getProperty(key)).isEqualTo("explicit-" + key);
        }
    }
}
