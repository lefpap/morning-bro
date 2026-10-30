package io.github.lefpap.morningbro.openmeteo;

import org.springframework.web.client.RestClientException;

public class OpenMeteoException extends RestClientException {

    public OpenMeteoException(String message) {
        super(message);
    }

}
