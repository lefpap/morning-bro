package io.github.lefpap.morningbro.weather;

import org.springframework.web.client.RestClientException;

/**
 * An error status from Open-Meteo, carrying its reason.
 */
public class OpenMeteoException extends RestClientException {

    public OpenMeteoException(String message) {
        super(message);
    }

}
