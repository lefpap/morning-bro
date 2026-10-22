package io.github.lefpap.morningbro.weather;

import java.util.Map;

/**
 * @param attribution the credit to the forecast source, which its terms require on the Weather Report
 */
public record WeatherSettings(
        int likelyRainPercent,
        int showRainPercent,
        Map<Condition, String> emoji,
        Map<TemperatureAlert, String> alertEmoji,
        String attribution) {
}
