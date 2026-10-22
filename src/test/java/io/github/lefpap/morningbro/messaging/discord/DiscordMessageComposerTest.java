package io.github.lefpap.morningbro.messaging.discord;

import io.github.lefpap.morningbro.messaging.CityForecast;
import io.github.lefpap.morningbro.messaging.MorningMessage;
import io.github.lefpap.morningbro.weather.City;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordMessageComposerTest {

    private static final City ATHENS = new City("Athens", "🇬🇷", 37.98, 23.73);
    private static final City BRUSSELS = new City("Brussels", "🇧🇪", 50.85, 4.35);

    private final DiscordMessageComposer composer = new DiscordMessageComposer(new DiscordSettings(
        "Morning Bro", Optional.of("https://example.com/coffee.png"), Optional.empty()));

    @Test
    void postsTheGreetingLinesThenOneCityForecastPerLineThenTheAttributionAsSubtext() {
        MorningMessage message = new MorningMessage.WithWeather(
            "Good Morning ☕",
            "☀️",
            Optional.of("Take it easy Friday! 😎"),
            List.of(
                new CityForecast(ATHENS, "☀️", 17, 27, OptionalInt.empty()),
                new CityForecast(BRUSSELS, "🌦️", 9, 14, OptionalInt.of(30))),
            "Forecast by Open-Meteo");

        assertThat(composer.compose(message)).isEqualTo(new WebhookPayload(
            "Morning Bro",
            "https://example.com/coffee.png",
            """
                Good Morning ☕☀️
                Take it easy Friday! 😎

                🇬🇷 **Athens** ☀️ 17–27°
                🇧🇪 **Brussels** 🌦️ 9–14° · ☔ 30%
                -# Forecast by Open-Meteo"""));
    }

    @Test
    void postsTheFallbackMessageAsTheGreetingLinesThenTheFallbackNote() {
        MorningMessage message = new MorningMessage.Fallback(
            "Good Morning ☕", Optional.of("Take it easy Friday! 😎"), "(weather is taking the day off)");

        assertThat(composer.compose(message)).isEqualTo(new WebhookPayload(
            "Morning Bro",
            "https://example.com/coffee.png",
            """
                Good Morning ☕
                Take it easy Friday! 😎
                (weather is taking the day off)"""));
    }
}
