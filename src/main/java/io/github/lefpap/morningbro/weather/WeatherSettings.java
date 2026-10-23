package io.github.lefpap.morningbro.weather;

import java.util.Map;

/**
 * @param attribution    the credit to the forecast source, which its terms require on the Weather Report
 * @param attributionUrl where the Attribution links to, as the forecast source's terms also require
 */
public record WeatherSettings(
        int unlikelyRainThreshold,
        int rainChanceDisplayThreshold,
        Map<Condition, String> emoji,
        Map<TemperatureAlert, String> alertEmoji,
        String attribution,
        String attributionUrl) {
}
