package io.github.lefpap.morningbro.messaging;

import java.util.List;
import java.util.Optional;

/**
 * The day's message as data, with the product rules applied and nothing formatted: each {@link MessagePublisher}
 * decides how to show it. It either carries the weather or is the Fallback Message.
 */
public sealed interface MorningMessage {

    /** The greeting text. */
    String greeting();

    /** The Friday Line, on Fridays only. */
    Optional<String> fridayLine();

    /**
     * @param homeWeather the Home City's Condition emoji and any Temperature Alert, which end the Greeting Line
     * @param attribution the credit to the forecast source, shown with the Weather Card
     */
    record WithWeather(
        String greeting,
        String homeWeather,
        Optional<String> fridayLine,
        List<CityForecast> weatherCard,
        String attribution) implements MorningMessage {
    }

    /** The message when the weather is unavailable: no Home City weather and no Weather Card. */
    record Fallback(String greeting, Optional<String> fridayLine, String fallbackNote) implements MorningMessage {
    }
}
