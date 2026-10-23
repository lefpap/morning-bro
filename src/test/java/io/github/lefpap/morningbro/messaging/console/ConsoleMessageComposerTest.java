package io.github.lefpap.morningbro.messaging.console;

import io.github.lefpap.morningbro.messaging.CityForecast;
import io.github.lefpap.morningbro.messaging.MorningMessage;
import io.github.lefpap.morningbro.weather.City;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleMessageComposerTest {

    private static final City ATHENS = new City("Athens", "🇬🇷", 37.98, 23.73);
    private static final City BRUSSELS = new City("Brussels", "🇧🇪", 50.85, 4.35);

    private final ConsoleMessageComposer composer = new ConsoleMessageComposer();

    @Test
    void showsTheGreetingLinesThenABlankLineThenOneCityForecastPerLineThenTheAttributionWithItsLink() {
        MorningMessage message = new MorningMessage.WithWeather(
                "Good Morning ☕",
                "☀️",
                Optional.of("Take it easy Friday! 😎"),
                List.of(
                        new CityForecast(ATHENS, "☀️", 17, 27, OptionalInt.empty()),
                        new CityForecast(BRUSSELS, "🌦️", 9, 14, OptionalInt.of(30))),
                "Weather data by Open-Meteo.com",
                "https://open-meteo.com/");

        assertThat(composer.compose(message)).isEqualTo("""
                Good Morning ☕☀️
                Take it easy Friday! 😎

                🇬🇷 Athens: ☀️ 17–27°
                🇧🇪 Brussels: 🌦️ 9–14° · ☔ 30%
                Weather data by Open-Meteo.com (https://open-meteo.com/)""");
    }

    @Test
    void showsTheFallbackMessageAsTheGreetingLinesThenTheFallbackNote() {
        MorningMessage message = new MorningMessage.Fallback(
                "Good Morning ☕", Optional.of("Take it easy Friday! 😎"), "(weather is taking the day off)");

        assertThat(composer.compose(message)).isEqualTo("""
                Good Morning ☕
                Take it easy Friday! 😎
                (weather is taking the day off)""");
    }
}
