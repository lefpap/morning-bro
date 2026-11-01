package io.github.lefpap.morningbro.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class DiscordClientConfigTest {

    // A closed local port, so nothing can reach Discord even if a test posts by mistake.
    static final String URL = "http://127.0.0.1:9/api/webhooks/123/secret-token";
    static final String BASE_URL = "spring.http.serviceclient.discord.base-url=";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            HttpClientAutoConfiguration.class,
            ImperativeHttpClientAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            RestClientAutoConfiguration.class,
            HttpServiceClientPropertiesAutoConfiguration.class,
            HttpServiceClientAutoConfiguration.class))
        .withUserConfiguration(DiscordClientConfig.class, DiscordMessagePublisher.class);

    @ParameterizedTest
    @ValueSource(strings = {"", "morning-bro.dry-run=false"})
    void realRunHasDiscordPublisherWithoutLoggingUrl(String dryRun, CapturedOutput output) {
        runner.withPropertyValues(BASE_URL + URL, dryRun).run(context -> {
            assertThat(context).hasSingleBean(DiscordMessagePublisher.class);
            assertThat(context).hasSingleBean(DiscordWebhookClient.class);
            assertThat(output).doesNotContain("secret-token");
        });
    }

    @Test
    void dryRunHasNoDiscordSideAndNeedsNoUrl() {
        runner.withPropertyValues("morning-bro.dry-run=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DiscordMessagePublisher.class);
            assertThat(context).doesNotHaveBean(DiscordWebhookClient.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void missingWebhookUrlFailsStartup(String webhookUrl) {
        runner.withPropertyValues(BASE_URL + webhookUrl).run(context ->
            assertThat(context).getFailure().rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Discord webhook URL is not set: set MORNING_BRO_DISCORD_WEBHOOK_URL, "
                    + "or MORNING_BRO_DRY_RUN=true to log the message instead"));
    }

}
