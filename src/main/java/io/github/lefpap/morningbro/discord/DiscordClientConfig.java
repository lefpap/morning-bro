package io.github.lefpap.morningbro.discord;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = DiscordClientConfig.GROUP, types = DiscordWebhookClient.class)
public class DiscordClientConfig {

    // Group settings are under spring.http.serviceclient.discord in application.yaml.
    static final String GROUP = "discord";

    @Bean
    public RestClientHttpServiceGroupConfigurer discordGroupConfigurer() {
        return groups -> groups.filterByName(GROUP)
            .forEachClient((_, clientBuilder) -> {
                clientBuilder.defaultStatusHandler(status -> !status.is2xxSuccessful(), (_, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String statusText = response.getStatusText();
                    throw new DiscordWebhookException("Webhook failed (" + statusCode + "): " + statusText);
                });

                clientBuilder.requestInterceptor(maskDiscordTokenInterceptor());
            });
    }

    private ClientHttpRequestInterceptor maskDiscordTokenInterceptor() {
        return (request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException ex) {
                // RestClient wraps an IOException in a ResourceAccessException whose message has the full URL,
                // so swap it for one without the URL.
                throw new DiscordWebhookException("Discord webhook request failed: " + ex.getClass().getSimpleName());
            }
        };
    }
}
