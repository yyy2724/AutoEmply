package health.autoemplyserver;

import health.autoemplyserver.config.EnvironmentBridge;
import health.autoemplyserver.service.AiModelState;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AutoEmplyServerApplication {

    public static void main(String[] args) {
        EnvironmentBridge.applyDotNetEnvironmentCompatibility();
        SpringApplication.run(AutoEmplyServerApplication.class, args);
    }

    @Bean
    AiModelState aiModelState() {
        return new AiModelState();
    }

}
