package io.github.lefpap.morningbro.message;

import io.github.lefpap.morningbro.MorningBroProperties;
import io.github.lefpap.morningbro.MorningBroProperties.Location;
import io.github.lefpap.morningbro.weather.Forecast;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MorningMessageComposerTest {

    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 29);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 30);

    private static final Location ATHENS = new Location("Athens", "GR", 37.98, 23.73);
    private static final Location IKARIA = new Location("Ikaria", "GR", 37.60, 26.17);
    private static final Location BRUSSELS = new Location("Brussels", "BE", 50.85, 4.35);
    private static final Location MILAN = new Location("Milan", "IT", 45.46, 9.19);

    private static final Forecast ATHENS_FORECAST = new Forecast("⛅", 15, 21, 25);
    private static final Forecast IKARIA_FORECAST = new Forecast("☀️", 18, 23, 0);
    private static final Forecast BRUSSELS_FORECAST = new Forecast("🌦️", 6, 11, 60);
    private static final Forecast MILAN_FORECAST = new Forecast("🌧️", 9, 14, 80);

    private static final MorningBroProperties PROPERTIES = new MorningBroProperties(
        ZoneId.of("Europe/Athens"),
        "Athens",
        new MorningBroProperties.Message("Good Morning — Καλημέρα! ☕", "Take it easy Friday! 😎"),
        List.of(ATHENS, IKARIA, BRUSSELS, MILAN), false);

    private static final String LOCATION_BLOCKS = """
        🇬🇷 Athens
        ⛅ · 15°–21° · 💧 25%

        🇬🇷 Ikaria
        ☀️ · 18°–23° · 💧 0%

        🇧🇪 Brussels
        🌦️ · 6°–11° · 💧 60%

        🇮🇹 Milan
        🌧️ · 9°–14° · 💧 80%""";

    private final MorningMessageComposer composer = new MorningMessageComposer(PROPERTIES);

    @Test
    void regularDay() {
        assertThat(composer.compose(THURSDAY, allPresent())).isEqualTo("""
            Good Morning — Καλημέρα! ☕⛅

            """ + LOCATION_BLOCKS);
    }

    @Test
    void friday() {
        assertThat(composer.compose(FRIDAY, allPresent())).isEqualTo("""
            Good Morning — Καλημέρα! ☕⛅ Take it easy Friday! 😎

            """ + LOCATION_BLOCKS);
    }

    @Test
    void failedLocation() {
        List<LocationWeather> weather = List.of(
            present(ATHENS, ATHENS_FORECAST),
            present(IKARIA, IKARIA_FORECAST),
            present(BRUSSELS, BRUSSELS_FORECAST),
            failed(MILAN));

        assertThat(composer.compose(THURSDAY, weather))
            .isEqualTo(composer.compose(THURSDAY, allPresent())
                .replace("🌧️ · 9°–14° · 💧 80%", "(weather is taking the day off)"));
    }

    @Test
    void failedHomeLocationOmitsEmoji() {
        List<LocationWeather> weather = List.of(
            failed(ATHENS),
            present(IKARIA, IKARIA_FORECAST),
            present(BRUSSELS, BRUSSELS_FORECAST),
            present(MILAN, MILAN_FORECAST));

        assertThat(firstLine(composer.compose(THURSDAY, weather)))
            .isEqualTo("Good Morning — Καλημέρα! ☕");
        assertThat(firstLine(composer.compose(FRIDAY, weather)))
            .isEqualTo("Good Morning — Καλημέρα! ☕ Take it easy Friday! 😎");
    }

    @Test
    void allLocationsFailed() {
        List<LocationWeather> weather = List.of(failed(ATHENS), failed(IKARIA), failed(BRUSSELS), failed(MILAN));

        assertThat(composer.compose(FRIDAY, weather)).isEqualTo("""
            Good Morning — Καλημέρα! ☕ Take it easy Friday! 😎

            🇬🇷 Athens
            (weather is taking the day off)

            🇬🇷 Ikaria
            (weather is taking the day off)

            🇧🇪 Brussels
            (weather is taking the day off)

            🇮🇹 Milan
            (weather is taking the day off)""");
    }

    @Test
    void homeEmojiComesFromHomeLocation() {
        List<LocationWeather> weather = List.of(
            present(BRUSSELS, BRUSSELS_FORECAST),
            present(ATHENS, ATHENS_FORECAST));

        assertThat(composer.compose(THURSDAY, weather)).isEqualTo("""
            Good Morning — Καλημέρα! ☕⛅

            🇧🇪 Brussels
            🌦️ · 6°–11° · 💧 60%

            🇬🇷 Athens
            ⛅ · 15°–21° · 💧 25%""");
    }

    @Test
    void negativeAndZeroValues() {
        assertThat(composer.compose(THURSDAY, List.of(present(BRUSSELS, new Forecast("🌨️", -4, 2, 0)))))
            .endsWith("\n🌨️ · -4°–2° · 💧 0%");
        assertThat(composer.compose(THURSDAY, List.of(present(BRUSSELS, new Forecast("☁️", -1, 0, 5)))))
            .endsWith("\n☁️ · -1°–0° · 💧 5%");
    }

    @Test
    void singleLocation() {
        assertThat(composer.compose(THURSDAY, List.of(present(ATHENS, ATHENS_FORECAST))))
            .isEqualTo("Good Morning — Καλημέρα! ☕⛅\n\n🇬🇷 Athens\n⛅ · 15°–21° · 💧 25%");
    }

    // The text blocks above could hide look-alikes (hyphen for en dash, bullet for middle dot); escapes can't.
    @Test
    void separatorsAreExactCodePoints() {
        String weatherLine = composer.compose(THURSDAY, List.of(present(ATHENS, ATHENS_FORECAST))).lines().toList().getLast();

        assertThat(weatherLine).isEqualTo("\u26C5 \u00B7 15\u00B0\u201321\u00B0 \u00B7 \uD83D\uDCA7 25%");
    }

    private static List<LocationWeather> allPresent() {
        return List.of(
            present(ATHENS, ATHENS_FORECAST),
            present(IKARIA, IKARIA_FORECAST),
            present(BRUSSELS, BRUSSELS_FORECAST),
            present(MILAN, MILAN_FORECAST));
    }

    private static LocationWeather present(Location location, Forecast forecast) {
        return new LocationWeather(location, Optional.of(forecast));
    }

    private static LocationWeather failed(Location location) {
        return new LocationWeather(location, Optional.empty());
    }

    private static String firstLine(String message) {
        return message.lines().findFirst().orElseThrow();
    }

}
