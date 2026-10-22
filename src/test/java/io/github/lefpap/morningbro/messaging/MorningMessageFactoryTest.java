package io.github.lefpap.morningbro.messaging;

import io.github.lefpap.morningbro.weather.City;
import io.github.lefpap.morningbro.weather.Condition;
import io.github.lefpap.morningbro.weather.Forecast;
import io.github.lefpap.morningbro.weather.TemperatureAlert;
import io.github.lefpap.morningbro.weather.WeatherSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class MorningMessageFactoryTest {

    private static final City ATHENS = new City("Athens", "🇬🇷", 37.98, 23.73);
    private static final City BRUSSELS = new City("Brussels", "🇧🇪", 50.85, 4.35);

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 21);
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 22);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);

    private final MorningMessageFactory factory = new MorningMessageFactory(
            new GreetingSettings("Good Morning ☕", "Athens", "Take it easy Friday! 😎", "(weather is taking the day off)"),
            new WeatherSettings(40, 20,
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
                    "Forecast by Open-Meteo"));

    @Test
    void theGreetingCarriesTheHomeCitysEmojiAndTemperatureAlert() {
        MorningMessage.WithWeather message = withWeather(factory.create(forecasts(
                ATHENS, forecast(WEDNESDAY, 0, 24, 37, null),
                BRUSSELS, forecast(WEDNESDAY, 71, -4, 1, null)), WEDNESDAY));

        assertThat(message.greeting()).isEqualTo("Good Morning ☕");
        assertThat(message.homeWeather()).isEqualTo("☀️🥵");
    }

    @Test
    void theFridayLineFollowsTheGreetingOnFridays() {
        MorningMessage message = factory.create(dryDay(FRIDAY), FRIDAY);

        assertThat(message.fridayLine()).contains("Take it easy Friday! 😎");
    }

    @Test
    void thereIsNoFridayLineOnOtherDays() {
        MorningMessage message = factory.create(dryDay(THURSDAY), THURSDAY);

        assertThat(message.fridayLine()).isEmpty();
    }

    @Test
    void theWeatherReportHasOneCityForecastPerCityWithTemperaturesRoundedToWholeDegrees() {
        MorningMessage.WithWeather message = withWeather(factory.create(forecasts(
                ATHENS, forecast(WEDNESDAY, 0, 17.4, 26.6, 0),
                BRUSSELS, forecast(WEDNESDAY, 71, -4.4, 0.5, 0)), WEDNESDAY));

        assertThat(message.weatherReport()).containsExactly(
                new CityForecast(ATHENS, "☀️", 17, 27, OptionalInt.empty()),
                new CityForecast(BRUSSELS, "🌨️🥶", -4, 1, OptionalInt.empty()));
    }

    @Test
    void theWeatherReportCarriesTheAttribution() {
        MorningMessage.WithWeather message = withWeather(factory.create(dryDay(WEDNESDAY), WEDNESDAY));

        assertThat(message.attribution()).isEqualTo("Forecast by Open-Meteo");
    }

    /** The Rain Chance Display Threshold is 20. An empty cell means no Rain Chance: in the forecast, or to show. */
    @ParameterizedTest
    @CsvSource({
            "20, 20",
            "19,   ",
            "  ,   "})
    void aCityForecastHasTheRainChanceOnlyAtOrAboveTheDisplayThreshold(Integer rainChance, Integer expected) {
        MorningMessage.WithWeather message = withWeather(factory.create(forecasts(
                ATHENS, forecast(WEDNESDAY, 0, 12, 24, 0),
                BRUSSELS, forecast(WEDNESDAY, 51, 9, 14, rainChance)), WEDNESDAY));

        assertThat(message.weatherReport()).last().extracting(CityForecast::rainChance)
                .isEqualTo(optional(expected));
    }

    @Test
    void withNoForecastsBecauseTheWeatherIsUnavailableTheFallbackMessageGoesOutInstead() {
        MorningMessage message = factory.create(Map.of(), FRIDAY);

        assertThat(message).isEqualTo(new MorningMessage.Fallback(
                "Good Morning ☕", Optional.of("Take it easy Friday! 😎"), "(weather is taking the day off)"));
    }

    @Test
    void whenTheHomeCityHasNoForecastTheFallbackMessageGoesOutInsteadOfAPartialWeatherReport() {
        Map<City, Forecast> brusselsOnly = Map.of(BRUSSELS, forecast(WEDNESDAY, 0, 12, 24, 0));

        MorningMessage message = factory.create(brusselsOnly, WEDNESDAY);

        assertThat(message).isEqualTo(new MorningMessage.Fallback(
                "Good Morning ☕", Optional.empty(), "(weather is taking the day off)"));
    }

    /** A clear, mild day everywhere. */
    private static Map<City, Forecast> dryDay(LocalDate day) {
        Forecast clear = forecast(day, 0, 12, 24, 0);
        return forecasts(ATHENS, clear, BRUSSELS, clear);
    }

    private static MorningMessage.WithWeather withWeather(MorningMessage message) {
        return assertThat(message).asInstanceOf(type(MorningMessage.WithWeather.class)).actual();
    }

    /** Ordered, since the Weather Report follows the order of the cities. */
    private static Map<City, Forecast> forecasts(City city, Forecast forecast, City other, Forecast otherForecast) {
        Map<City, Forecast> forecasts = new LinkedHashMap<>();
        forecasts.put(city, forecast);
        forecasts.put(other, otherForecast);
        return forecasts;
    }

    private static Forecast forecast(LocalDate day, int weatherCode, double low, double high, Integer rainChance) {
        return new Forecast(day, weatherCode, low, high, optional(rainChance));
    }

    private static OptionalInt optional(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
