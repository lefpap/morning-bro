package io.github.lefpap.morningbro.weather;

/** The forecast couldn't be fetched, or came back partial or unexpected. The message is written for the Owner. */
public class WeatherException extends RuntimeException {

    WeatherException(String message) {
        super(message);
    }
}
