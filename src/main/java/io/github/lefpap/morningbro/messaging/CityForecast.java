package io.github.lefpap.morningbro.messaging;

import io.github.lefpap.morningbro.weather.City;

import java.util.OptionalInt;

/**
 * One city's entry in the Weather Card.
 *
 * @param city       the city, for its flag and name
 * @param emoji      the Condition emoji followed by any Temperature Alert
 * @param low        the low, rounded to whole degrees
 * @param high       the high, rounded to whole degrees
 * @param rainChance the Rain Chance, only when it is at or above the Rain Chance Display Threshold
 */
public record CityForecast(City city, String emoji, int low, int high, OptionalInt rainChance) {
}
