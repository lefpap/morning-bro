package io.github.lefpap.morningbro.messaging.discord;

import io.github.lefpap.morningbro.messaging.CityForecast;
import io.github.lefpap.morningbro.messaging.MorningMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * The Morning Message as a Discord webhook payload, posted under the Morning Bro identity. It is all plain text with
 * Discord's markdown: the greeting lines first, so they show in phone notifications, then a blank line, one City
 * Forecast per line, and the Attribution as small subtext.
 */
public class DiscordMessageComposer {

    private final DiscordSettings settings;

    public DiscordMessageComposer(DiscordSettings settings) {
        this.settings = settings;
    }

    public WebhookPayload compose(MorningMessage message) {
        List<String> lines = new ArrayList<>();
        switch (message) {
            case MorningMessage.WithWeather(
                var greeting, var homeWeather, var fridayLine, var weatherReport, var attribution) -> {
                lines.add(greeting + homeWeather);
                fridayLine.ifPresent(lines::add);
                lines.add("");
                weatherReport.forEach(cityForecast -> lines.add(line(cityForecast)));
                lines.add("-# " + attribution);
            }
            case MorningMessage.Fallback(var greeting, var fridayLine, var fallbackNote) -> {
                lines.add(greeting);
                fridayLine.ifPresent(lines::add);
                lines.add(fallbackNote);
            }
        }
        return new WebhookPayload(settings.username(), settings.avatarUrl().orElse(null), String.join("\n", lines));
    }

    private static String line(CityForecast cityForecast) {
        String line = "%s **%s** %s %d–%d°".formatted(
            cityForecast.city().flag(), cityForecast.city().name(),
            cityForecast.emoji(), cityForecast.low(), cityForecast.high());
        if (cityForecast.rainChance().isPresent()) {
            line += " · ☔ %d%%".formatted(cityForecast.rainChance().getAsInt());
        }
        return line;
    }
}
