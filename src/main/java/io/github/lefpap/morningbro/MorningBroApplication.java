package io.github.lefpap.morningbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MorningBroApplication {

    static void main(String[] args) {
        SpringApplication.run(MorningBroApplication.class, args);
    }

}
