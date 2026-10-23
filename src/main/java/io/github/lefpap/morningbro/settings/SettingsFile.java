package io.github.lefpap.morningbro.settings;

import io.github.lefpap.morningbro.weather.City;

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
            int unlikelyRainThreshold,
            int rainChanceDisplayThreshold,
            Map<String, String> emoji,
            Map<String, String> alertEmoji,
            String attribution,
            String attributionUrl) {
    }

    record Discord(String username, Optional<String> avatarUrl) {
    }
}
