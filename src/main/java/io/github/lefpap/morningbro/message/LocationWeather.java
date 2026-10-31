package io.github.lefpap.morningbro.message;

import io.github.lefpap.morningbro.MorningBroProperties;
import io.github.lefpap.morningbro.weather.Forecast;

import java.util.Optional;

// One location's weather for today; an empty forecast means the weather is unavailable.
public record LocationWeather(MorningBroProperties.Location location, Optional<Forecast> forecast) {
}
