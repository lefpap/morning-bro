package io.github.lefpap.morningbro.weather;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class WeatherEmojiTest {

    /** Unlikely Rain gets its own emoji here, so it can't be mistaken for Drizzle. */
    private final WeatherEmoji weatherEmoji = new WeatherEmoji(new WeatherSettings(40, 20,
            Map.ofEntries(
                    entry(Condition.CLEAR, "☀️"),
                    entry(Condition.MOSTLY_CLEAR, "🌤️"),
                    entry(Condition.PARTLY_CLOUDY, "⛅"),
                    entry(Condition.OVERCAST, "☁️"),
                    entry(Condition.FOG, "🌫️"),
                    entry(Condition.DRIZZLE, "🌦️"),
                    entry(Condition.RAIN, "🌧️"),
                    entry(Condition.SNOW, "🌨️"),
                    entry(Condition.THUNDERSTORM, "⛈️"),
                    entry(Condition.UNKNOWN, "🌡️"),
                    entry(Condition.UNLIKELY_RAIN, "🌂")),
            Map.of(TemperatureAlert.HOT, "🥵", TemperatureAlert.FREEZING, "🥶"),
            "Weather data by Open-Meteo.com",
            "https://open-meteo.com/"));

    /** Every weather code Open-Meteo documents, grouped by the Condition it means. */
    @ParameterizedTest
    @CsvSource({
            "0, CLEAR",
            "1, MOSTLY_CLEAR",
            "2, PARTLY_CLOUDY",
            "3, OVERCAST",
            "45, FOG", "48, FOG",
            "51, DRIZZLE", "53, DRIZZLE", "55, DRIZZLE", "56, DRIZZLE", "57, DRIZZLE",
            "61, RAIN", "63, RAIN", "65, RAIN", "66, RAIN", "67, RAIN", "80, RAIN", "81, RAIN", "82, RAIN",
            "71, SNOW", "73, SNOW", "75, SNOW", "77, SNOW", "85, SNOW", "86, SNOW",
            "95, THUNDERSTORM", "96, THUNDERSTORM", "99, THUNDERSTORM"})
    void eachWeatherCodeMapsToItsCondition(int weatherCode, Condition expected) {
        assertThat(weatherEmoji.condition(forecast(weatherCode))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 100, -1})
    void anUnrecognizedWeatherCodeIsUnknown(int weatherCode) {
        assertThat(weatherEmoji.condition(forecast(weatherCode))).isEqualTo(Condition.UNKNOWN);
    }

    /** The Unlikely Rain Threshold is 40. An empty Rain Chance means the forecast has none. */
    @ParameterizedTest
    @CsvSource({
            "61, 39, UNLIKELY_RAIN",
            "95, 39, UNLIKELY_RAIN",
            "61, 40, RAIN",
            "95, 40, THUNDERSTORM",
            "61,   , RAIN",
            "95,   , THUNDERSTORM",
            "51, 10, DRIZZLE",
            "71, 10, SNOW"})
    void rainOrAThunderstormBelowTheThresholdIsUnlikelyRain(int weatherCode, Integer rainChance, Condition expected) {
        assertThat(weatherEmoji.condition(forecast(weatherCode, rainChance))).isEqualTo(expected);
    }

    /** Hot is a high of 35° or more; Freezing is a low of 0° or below. An empty cell means no alert. */
    @ParameterizedTest
    @CsvSource({
            "24,   35, HOT",
            "24, 34.9,    ",
            " 0,   10, FREEZING",
            "0.1,  10,    "})
    void anExtremeTemperatureAddsATemperatureAlert(double low, double high, TemperatureAlert expected) {
        assertThat(weatherEmoji.alerts(forecast(0, null, low, high))).isEqualTo(expected == null ? Set.of() : Set.of(expected));
    }

    @ParameterizedTest
    @CsvSource({
            " 0,   , 24, 37, ☀️🥵",
            "71,   , -4,  1, 🌨️🥶",
            "61, 30, 12, 20, 🌂",
            " 0,   , 12, 24, ☀️"})
    void theEmojiIsTheConditionFollowedByAnyTemperatureAlert(
            int weatherCode, Integer rainChance, double low, double high, String expected) {
        assertThat(weatherEmoji.emoji(forecast(weatherCode, rainChance, low, high))).isEqualTo(expected);
    }

    private static Forecast forecast(int weatherCode) {
        return forecast(weatherCode, null);
    }

    private static Forecast forecast(int weatherCode, Integer rainChance) {
        return forecast(weatherCode, rainChance, 12.0, 24.0);
    }

    private static Forecast forecast(int weatherCode, Integer rainChance, double low, double high) {
        return new Forecast(LocalDate.of(2026, 10, 21), weatherCode, low, high,
                rainChance == null ? OptionalInt.empty() : OptionalInt.of(rainChance));
    }
}
