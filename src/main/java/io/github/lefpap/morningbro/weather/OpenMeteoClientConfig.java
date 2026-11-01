package io.github.lefpap.morningbro.weather;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import java.nio.charset.StandardCharsets;

/**
 * Registers the {@code openmeteo} service group and turns error statuses into {@link OpenMeteoException}.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = OpenMeteoClientConfig.GROUP, types = OpenMeteoClient.class)
class OpenMeteoClientConfig {

    static final String GROUP = "openmeteo";

    @Bean
    RestClientHttpServiceGroupConfigurer openMeteoGroupConfigurer() {
        return groups -> groups.filterByName(GROUP)
            .forEachClient((_, clientBuilder) ->
                clientBuilder.defaultStatusHandler(HttpStatusCode::isError, (_, response) -> {
                    // The body carries Open-Meteo's reason, e.g. {"error":true,"reason":"..."}, so keep it for the logs.
                    String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    throw new OpenMeteoException("Open-Meteo request failed (" + response.getStatusCode() + ")"
                            + (body.isBlank() ? "" : ": " + body));
                }));
    }

}
