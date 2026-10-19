package io.github.lefpap.morningbro.weather;

import java.util.Map;

public record WeatherSettings(
        int likelyRainPercent,
        int showRainPercent,
        Map<Condition, String> emoji,
        Map<TemperatureAlert, String> alertEmoji) {
}
