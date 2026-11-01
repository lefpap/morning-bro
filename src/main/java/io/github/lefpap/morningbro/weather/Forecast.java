package io.github.lefpap.morningbro.weather;

/**
 * Today's weather for one location, ready for the message: temperatures in whole °C, rain chance in percent.
 */
public record Forecast(String emoji, int minTemperature, int maxTemperature, int rainChance) {
}
