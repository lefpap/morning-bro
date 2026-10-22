package io.github.lefpap.morningbro.messaging.discord;

import com.sun.net.httpserver.HttpServer;
import io.github.lefpap.morningbro.messaging.CityForecast;
import io.github.lefpap.morningbro.messaging.MorningMessage;
import io.github.lefpap.morningbro.weather.City;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a local server standing in for the Discord webhook. No calls leave the machine.
 */
class DiscordMessagePublisherTest {

    private static final String WEBHOOK_PATH = "/api/webhooks/1/token";

    private static final MorningMessage MESSAGE = new MorningMessage.WithWeather(
        "Good Morning ☕",
        "☀️",
        Optional.empty(),
        List.of(new CityForecast(new City("Athens", "🇬🇷", 37.98, 23.73), "☀️", 17, 27, OptionalInt.empty())),
        "Forecast by Open-Meteo");

    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer discord;
    private String webhookUrl;

    record Request(String method, String path, String contentType, String body) {
    }

    @BeforeEach
    void startDiscord() throws IOException {
        discord = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        discord.start();
        InetSocketAddress address = discord.getAddress();
        webhookUrl = "http://" + address.getHostString() + ":" + address.getPort() + WEBHOOK_PATH;
    }

    @AfterEach
    void stopDiscord() {
        discord.stop(0);
    }

    @Test
    void postsThePayloadAsJsonToTheWebhook() {
        respondWith(204, "");

        publisher(false).publish(MESSAGE);

        Request request = assertThat(requests).singleElement().actual();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(WEBHOOK_PATH);
        assertThat(request.contentType()).isEqualTo("application/json");
        JsonMapper json = new JsonMapper();
        assertThat(json.readTree(request.body())).isEqualTo(json.readTree("""
            {
              "username": "Morning Bro",
              "avatar_url": "https://example.com/coffee.png",
              "content": "Good Morning ☕☀️\\n\\n🇬🇷 **Athens** ☀️ 17–27°\\n-# Forecast by Open-Meteo"
            }"""));
    }

    @Test
    void failsWithDiscordsAnswerWhenThePostIsRejected() {
        respondWith(400, "{\"message\": \"Cannot send an empty message\", \"code\": 50006}");

        assertThatThrownBy(() -> publisher(false).publish(MESSAGE))
            .isInstanceOf(DiscordException.class)
            .hasMessageContaining("Discord rejected the post with HTTP 400")
            .hasMessageContaining("Cannot send an empty message");
    }

    @Test
    void postsNothingInADryRunEvenWithAWebhookUrl() {
        respondWith(204, "");

        publisher(true).publish(MESSAGE);

        assertThat(requests).isEmpty();
    }

    @Test
    void needsNoWebhookUrlInADryRun() {
        DiscordMessagePublisher publisher = publisher(true, Optional.empty());

        assertThatNoException().isThrownBy(() -> publisher.publish(MESSAGE));
    }

    /**
     * Records each request, and answers it with this status and body.
     */
    private void respondWith(int status, String body) {
        discord.createContext("/", exchange -> {
            try (exchange) {
                requests.add(new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                byte[] response = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
                exchange.getResponseBody().write(response);
            }
        });
    }

    private DiscordMessagePublisher publisher(boolean dryRun) {
        return publisher(dryRun, Optional.of(webhookUrl));
    }

    private DiscordMessagePublisher publisher(boolean dryRun, Optional<String> webhookUrl) {
        DiscordSettings settings = new DiscordSettings(
            "Morning Bro", Optional.of("https://example.com/coffee.png"), webhookUrl);
        return new DiscordMessagePublisher(new DiscordMessageComposer(settings), settings, dryRun);
    }
}
