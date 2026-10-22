package io.github.lefpap.morningbro.greeting.console;

import io.github.lefpap.morningbro.greeting.CityForecast;
import io.github.lefpap.morningbro.greeting.MorningMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * The Morning Message as plain text for the console: the greeting lines, then either the Fallback Note or a blank line
 * and one City Forecast per line.
 */
public class ConsoleComposer {

    public String compose(MorningMessage message) {
        List<String> lines = new ArrayList<>();
        switch (message) {
            case MorningMessage.WithWeather(var greeting, var homeWeather, var fridayLine, var weatherCard) -> {
                lines.add(greeting + homeWeather);
                fridayLine.ifPresent(lines::add);
                lines.add("");
                weatherCard.forEach(cityForecast -> lines.add(line(cityForecast)));
            }
            case MorningMessage.Fallback(var greeting, var fridayLine, var fallbackNote) -> {
                lines.add(greeting);
                fridayLine.ifPresent(lines::add);
                lines.add(fallbackNote);
            }
        }
        return String.join("\n", lines);
    }

    private static String line(CityForecast cityForecast) {
        String line = "%s %s: %s %d–%d°".formatted(
            cityForecast.city().flag(), cityForecast.city().name(),
            cityForecast.emoji(), cityForecast.low(), cityForecast.high());
        if (cityForecast.rainChance().isPresent()) {
            line += " · ☔ %d%%".formatted(cityForecast.rainChance().getAsInt());
        }
        return line;
    }
}
