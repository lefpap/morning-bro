package io.github.lefpap.morningbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MorningBroApplication {

    static void main(String[] args) {
        SpringApplication.run(MorningBroApplication.class, args);
    }

    // Injected so tests can fix "now"; the job applies the configured time zone itself.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

}
