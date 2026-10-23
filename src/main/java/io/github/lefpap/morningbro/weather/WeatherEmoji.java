package io.github.lefpap.morningbro.weather;

import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The weather rules: a Forecast becomes a Condition and any Temperature Alerts, shown as emoji from the settings.
 */
public class WeatherEmoji {

    private final WeatherSettings settings;

    public WeatherEmoji(WeatherSettings settings) {
        this.settings = settings;
    }

    /** The Condition's emoji followed by the emoji of any Temperature Alert, as the Settings File sets them. */
    public String emoji(Forecast forecast) {
        StringBuilder emoji = new StringBuilder(settings.emoji().get(condition(forecast)));
        alerts(forecast).forEach(alert -> emoji.append(settings.alertEmoji().get(alert)));
        return emoji.toString();
    }

    public Condition condition(Forecast forecast) {
        Condition condition = conditionOf(forecast.weatherCode());
        return switch (condition) {
            case Condition c when isRainy(c) && isUnlikely(forecast) -> Condition.UNLIKELY_RAIN;
            case Condition c -> c;
        };
    }

    /** The Temperature Alerts for the day, shown alongside the Condition, never instead of it. */
    public Set<TemperatureAlert> alerts(Forecast forecast) {
        Set<TemperatureAlert> alerts = EnumSet.noneOf(TemperatureAlert.class);
        if (forecast.high() >= 35) {
            alerts.add(TemperatureAlert.HOT);
        }
        if (forecast.low() <= 0) {
            alerts.add(TemperatureAlert.FREEZING);
        }
        return alerts;
    }

    private static boolean isRainy(Condition condition) {
        return condition == Condition.RAIN || condition == Condition.THUNDERSTORM;
    }

    private boolean isUnlikely(Forecast forecast) {
        OptionalInt rainChance = forecast.rainChance();
        return rainChance.isPresent() && rainChance.getAsInt() < settings.unlikelyRainThreshold();
    }

    private static Condition conditionOf(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> Condition.CLEAR;
            case 1 -> Condition.MOSTLY_CLEAR;
            case 2 -> Condition.PARTLY_CLOUDY;
            case 3 -> Condition.OVERCAST;
            case 45, 48 -> Condition.FOG;
            case 51, 53, 55, 56, 57 -> Condition.DRIZZLE;
            case 61, 63, 65, 66, 67, 80, 81, 82 -> Condition.RAIN;
            case 71, 73, 75, 77, 85, 86 -> Condition.SNOW;
            case 95, 96, 99 -> Condition.THUNDERSTORM;
            default -> Condition.UNKNOWN;
        };
    }
}
