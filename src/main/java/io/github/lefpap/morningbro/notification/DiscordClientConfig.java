package io.github.lefpap.morningbro.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import java.io.IOException;

// The Discord client exists only on a real run, so a dry run needs no webhook URL.
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "morning-bro.dry-run", havingValue = false, matchIfMissing = true)
@ImportHttpServices(group = DiscordClientConfig.GROUP, types = DiscordWebhookClient.class)
class DiscordClientConfig {

    // Group settings are under spring.http.serviceclient.discord in application.yaml.
    static final String GROUP = "discord";

    // Without this check a missing URL only shows up at post time, after all the weather calls.
    // Blank counts as missing: an unset GitHub Actions secret arrives as an empty env var.
    DiscordClientConfig(@Value("${spring.http.serviceclient.discord.base-url:}") String webhookUrl) {
        if (!StringUtils.hasText(webhookUrl)) {
            throw new IllegalStateException("Discord webhook URL is not set: set MORNING_BRO_DISCORD_WEBHOOK_URL, "
                + "or MORNING_BRO_DRY_RUN=true to log the message instead");
        }
    }

    @Bean
    RestClientHttpServiceGroupConfigurer discordGroupConfigurer() {
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
