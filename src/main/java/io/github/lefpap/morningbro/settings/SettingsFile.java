package io.github.lefpap.morningbro.settings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The Settings File exactly as written, before validation. */
record SettingsFile(Schedule schedule, List<City> cities, Greeting greeting, Weather weather, Discord discord) {

    record Schedule(int hour, String timezone) {
    }

    record Greeting(String text, String homeCity, String friday, String fallback) {
    }

    record Weather(
            int likelyRainPercent,
            int showRainPercent,
            Map<String, String> emoji,
            Map<String, String> alertEmoji) {
    }

    record Discord(String username, Optional<String> avatarUrl, String color, String footer) {
    }
}
