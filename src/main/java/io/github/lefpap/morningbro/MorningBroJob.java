package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.message.LocationWeather;
import io.github.lefpap.morningbro.message.MorningMessageComposer;
import io.github.lefpap.morningbro.notification.DiscordWebhookClient;
import io.github.lefpap.morningbro.weather.Forecast;
import io.github.lefpap.morningbro.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(name = "morning-bro.run-on-startup", matchIfMissing = true)
public class MorningBroJob implements ApplicationRunner {

    private final MorningBroProperties properties;
    private final WeatherService weather;
    private final MorningMessageComposer composer;
    private final DiscordWebhookClient discord;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<LocationWeather> todaysWeather = new ArrayList<>();
        for (MorningBroProperties.Location location : properties.locations()) {
            Optional<Forecast> forecast = weather.today(location);
            log.info("Forecast for {}: {}", location.name(), forecast.map(Forecast::toString).orElse("N/A"));
            todaysWeather.add(new LocationWeather(location, forecast));
        }

        String message = composer.compose(LocalDate.now(properties.timeZone()), todaysWeather);
        log.info("Posting morning message:\n{}", message);
        discord.postText(message);
    }

}
