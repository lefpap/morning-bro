package io.github.lefpap.morningbro.messaging.discord;

import io.github.lefpap.morningbro.messaging.MessagePublisher;
import io.github.lefpap.morningbro.messaging.MorningMessage;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Posts the Morning Message to the Channel through the Discord webhook. In a Dry Run, prints the payload instead. */
public class DiscordMessagePublisher implements MessagePublisher {

    private static final JsonMapper MAPPER = new JsonMapper();

    /**
     * How long to wait for Discord before giving up, so a hung connection can't stall the morning run.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final DiscordMessageComposer composer;
    private final DiscordSettings settings;
    private final boolean dryRun;

    public DiscordMessagePublisher(DiscordMessageComposer composer, DiscordSettings settings, boolean dryRun) {
        this.composer = composer;
        this.settings = settings;
        this.dryRun = dryRun;
    }

    @Override
    public void publish(MorningMessage message) {
        WebhookPayload payload = composer.compose(message);
        if (dryRun) {
            IO.println("Dry Run, so nothing is posted. The payload for Discord:");
            IO.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.webhookUrl().orElseThrow()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new DiscordException("Discord rejected the post with HTTP %d: %s"
                    .formatted(response.statusCode(), response.body()));
            }
        } catch (IOException e) {
            // HttpClient often leaves the message empty, so the exception's name says what went wrong.
            throw new DiscordException("Cannot reach Discord: " + e);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new DiscordException("Interrupted while posting to Discord");
        }
    }
}
