package io.github.lefpap.morningbro.weather;

/**
 * Emoji for WMO weather codes, as returned in Open-Meteo's daily {@code weather_code}.
 * Escaped so the variation selector (U+FE0F) is visible: every emoji but ⛅ carries one.
 */
final class WeatherEmoji {

    static final String SUN = "\u2600\uFE0F";
    static final String PARTLY_CLOUDY = "\u26C5";
    static final String CLOUD = "\u2601\uFE0F";
    static final String FOG = "\uD83C\uDF2B\uFE0F";
    static final String SHOWERS = "\uD83C\uDF26\uFE0F";
    static final String RAIN = "\uD83C\uDF27\uFE0F";
    static final String SNOW = "\uD83C\uDF28\uFE0F";
    static final String THUNDERSTORM = "\u26C8\uFE0F";
    static final String UNKNOWN = "\uD83C\uDF21\uFE0F";

    private WeatherEmoji() {
    }

    static String forCode(int code) {
        return switch (code) {
            case 0 -> SUN;
            case 1, 2 -> PARTLY_CLOUDY;
            case 3 -> CLOUD;
            case 45, 48 -> FOG;
            case 51, 52, 53, 54, 55, 56, 57, 80, 81, 82 -> SHOWERS;
            case 61, 62, 63, 64, 65, 66, 67 -> RAIN;
            case 71, 72, 73, 74, 75, 76, 77, 85, 86 -> SNOW;
            case 95, 96, 97, 98, 99 -> THUNDERSTORM;
            default -> UNKNOWN;
        };
    }

}
