package io.github.lefpap.morningbro.weather;

import java.util.OptionalInt;

/** One city's weather for the Forecast Day, as the forecast source reports it. */
public record Forecast(int weatherCode, double low, double high, OptionalInt rainChance) {
}
