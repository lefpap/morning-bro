package io.github.lefpap.morningbro.greeting;

import io.github.lefpap.morningbro.weather.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Builds the Morning Message for the Forecast Day, applying the product rules once for every {@link MessagePublisher}.
 */
public class MorningMessageFactory {

    private final GreetingSettings greeting;
    private final WeatherEmoji weatherEmoji;
    private final int showRainPercent;

    public MorningMessageFactory(GreetingSettings greeting, WeatherSettings weather) {
        this.greeting = greeting;
        this.weatherEmoji = new WeatherEmoji(weather);
        this.showRainPercent = weather.showRainPercent();
    }

    /**
     * The Morning Message, or the Fallback Message if there is no forecast for the Home City: the weather is
     * unavailable (no forecasts at all) or doesn't make a whole message.
     *
     * @param forecastsByCity one Forecast per city, or none if the weather is unavailable
     */
    public MorningMessage create(Map<City, Forecast> forecastsByCity, LocalDate forecastDay) {
        Optional<String> fridayLine = fridayLine(forecastDay);
        return homeForecast(forecastsByCity)
            .map(home -> withWeather(forecastsByCity, home, fridayLine))
            .orElseGet(() -> new MorningMessage.Fallback(greeting.text(), fridayLine, greeting.fallback()));
    }

    private MorningMessage withWeather(Map<City, Forecast> forecastsByCity, Forecast home, Optional<String> fridayLine) {
        List<CityForecast> weatherCard = forecastsByCity.entrySet().stream()
            .map(entry -> cityForecast(entry.getKey(), entry.getValue()))
            .toList();
        return new MorningMessage.WithWeather(greeting.text(), weatherEmoji.emoji(home), fridayLine, weatherCard);
    }

    private Optional<Forecast> homeForecast(Map<City, Forecast> forecastsByCity) {
        return forecastsByCity.entrySet().stream()
            .filter(entry -> entry.getKey().name().equals(greeting.homeCity()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private CityForecast cityForecast(City city, Forecast forecast) {
        OptionalInt rainChance = forecast.rainChance();
        boolean showRainChance = rainChance.isPresent() && rainChance.getAsInt() >= showRainPercent;
        return new CityForecast(
            city,
            weatherEmoji.emoji(forecast),
            (int) Math.round(forecast.low()),
            (int) Math.round(forecast.high()),
            showRainChance ? rainChance : OptionalInt.empty());
    }

    private Optional<String> fridayLine(LocalDate forecastDay) {
        return DayOfWeek.FRIDAY.equals(forecastDay.getDayOfWeek())
            ? Optional.of(greeting.friday())
            : Optional.empty();
    }
}
