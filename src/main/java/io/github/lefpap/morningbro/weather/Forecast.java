package io.github.lefpap.morningbro.weather;

import java.time.LocalDate;
import java.util.OptionalInt;

/** One city's weather for a day, as the forecast source reports it. */
public record Forecast(LocalDate day, int weatherCode, double low, double high, OptionalInt rainChance) {
}
