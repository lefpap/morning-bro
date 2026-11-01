package io.github.lefpap.morningbro.weather;

import org.springframework.web.client.RestClientException;

/**
 * An error status from Open-Meteo, carrying its reason.
 */
class OpenMeteoException extends RestClientException {

    OpenMeteoException(String message) {
        super(message);
    }

}
