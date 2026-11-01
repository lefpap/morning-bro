package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.message.LocationWeather;
import io.github.lefpap.morningbro.message.MorningMessageComposer;
import io.github.lefpap.morningbro.notification.MessagePublisher;
import io.github.lefpap.morningbro.weather.Forecast;
import io.github.lefpap.morningbro.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
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
    // No publisher bean exists on a dry run (no Discord client or webhook URL), so it's looked up only when posting.
    private final ObjectProvider<MessagePublisher> publisher;
    private final Clock clock;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<LocationWeather> todaysWeather = properties.locations().stream()
            .map(this::fetchWeather)
            .toList();

        // "Today" is the date in the configured zone, never the clock's or the JVM's zone.
        LocalDate today = LocalDate.ofInstant(clock.instant(), properties.timeZone());
        String message = composer.compose(today, todaysWeather);
        long withWeather = todaysWeather.stream().filter(entry -> entry.forecast().isPresent()).count();

        if (properties.dryRun()) {
            log.info("Dry run, not posting morning message:\n{}", message);
            log.info("Dry run done with weather for {} of {} locations", withWeather, todaysWeather.size());
            return;
        }

        publisher.getObject().publish(message);
        log.info("Morning message published with weather for {} of {} locations", withWeather, todaysWeather.size());
    }

    private LocationWeather fetchWeather(MorningBroProperties.Location location) {
        Optional<Forecast> forecast;
        try {
            forecast = weather.today(location);
            log.info("Forecast for {}: {}", location.name(), forecast.map(Forecast::toString).orElse("N/A"));
        } catch (RuntimeException ex) {
            // A weather failure must never stop the message: the location shows the "day off" line instead.
            log.warn("Weather unavailable for {}: {}", location.name(), ex.toString());
            forecast = Optional.empty();
        }
        return new LocationWeather(location, forecast);
    }

}
