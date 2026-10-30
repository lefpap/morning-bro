package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.discord.DiscordWebhookClient;
import io.github.lefpap.morningbro.openmeteo.OpenMeteoClient;
import io.github.lefpap.morningbro.openmeteo.OpenMeteoForecast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(name = "morning-bro.run-on-startup", matchIfMissing = true)
public class MorningBroJob implements ApplicationRunner {

    private final MorningBroProperties properties;
    private final OpenMeteoClient openMeteo;
    private final DiscordWebhookClient discord;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (MorningBroProperties.Location location : properties.locations()) {
            OpenMeteoForecast forecast = openMeteo.today(location.latitude(), location.longitude(), properties.timeZone());
            log.info("Forecast for {}: {}", location.name(), forecast);
        }

        discord.postText("Morning Bro is alive ✅");
    }

}
