package io.github.lefpap.morningbro.weather;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.springframework.util.CollectionUtils.firstElement;

final class ForecastConverter {

    private ForecastConverter() {
    }

    // Reads the first day (the request asks for exactly one). Any missing value means no forecast at all.
    static Optional<Forecast> convert(@Nullable OpenMeteoForecast response) {
        if (response == null || response.daily() == null) {
            return Optional.empty();
        }
        OpenMeteoForecast.Daily daily = response.daily();
        Integer code = firstElement(daily.weatherCode());
        Double max = firstElement(daily.temperatureMax());
        Double min = firstElement(daily.temperatureMin());
        Integer rain = firstElement(daily.precipitationProbabilityMax());
        if (code == null || max == null || min == null || rain == null) {
            return Optional.empty();
        }

        Forecast forecast = new Forecast(WeatherEmoji.forCode(code), round(min), round(max), rain);
        return Optional.of(forecast);
    }

    // Half-up, ties away from zero (-2.5 → -3). An int can't be negative zero.
    private static int round(double temperature) {
        return BigDecimal.valueOf(temperature).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

}
