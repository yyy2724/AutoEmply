package health.autoemplyserver.config;

import health.autoemplyserver.service.prompt.PromptPresetSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(PromptPresetSeeder promptPresetSeeder) {
        return args -> promptPresetSeeder.seed();
    }
}
