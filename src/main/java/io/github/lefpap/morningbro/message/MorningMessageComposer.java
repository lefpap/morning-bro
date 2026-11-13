package io.github.lefpap.morningbro.message;

import io.github.lefpap.morningbro.MorningBroProperties;
import io.github.lefpap.morningbro.weather.Forecast;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the morning message text. It is pure: it takes "today" as an argument and never reads the clock.
 */
@Component
@RequiredArgsConstructor
public class MorningMessageComposer {

    private static final String BLOCK_SEPARATOR = "\n\n";
    private static final String WEATHER_UNAVAILABLE = "(weather is taking the day off)";

    private final MorningBroProperties properties;

    /**
     * The full message for today: a greeting, one block per location in the given order, then the weather data credit.
     */
    public String compose(LocalDate today, List<LocationWeather> weather) {
        Stream<String> blocks = weather.stream().map(MorningMessageComposer::locationBlock);
        String header = header(today, weather);
        String attribution = properties.message().attribution();
        return Stream.of(Stream.of(header), blocks, Stream.of(attribution))
            .flatMap(Function.identity())
            .collect(Collectors.joining(BLOCK_SEPARATOR));
    }

    private static String locationBlock(LocationWeather entry) {
        String title = CountryFlag.of(entry.location().countryCode()) + " " + entry.location().name();
        String weatherLine = entry.forecast()
            .map(MorningMessageComposer::weatherLine)
            .orElse(WEATHER_UNAVAILABLE);
        return title + "\n" + weatherLine;
    }

    private String header(LocalDate today, List<LocationWeather> weather) {
        String greeting = properties.message().morning() + homeEmoji(weather);
        return isFriday(today) ? greeting + " " + properties.message().friday() : greeting;
    }

    // Empty when the home location has no forecast, so the greeting simply goes without it.
    private String homeEmoji(List<LocationWeather> weather) {
        return weather.stream()
            .filter(entry -> entry.location().name().equals(properties.home()))
            .findFirst()
            .flatMap(LocationWeather::forecast)
            .map(Forecast::emoji)
            .orElse("");
    }

    private static boolean isFriday(LocalDate date) {
        return DayOfWeek.FRIDAY.equals(date.getDayOfWeek());
    }

    // Locale.ROOT keeps ASCII digits and minus sign whatever the JVM's default locale.
    private static String weatherLine(Forecast forecast) {
        return String.format(Locale.ROOT, "%s · %d°–%d° · 💧 %d%%",
            forecast.emoji(), forecast.minTemperature(), forecast.maxTemperature(), forecast.rainChance());
    }

}
