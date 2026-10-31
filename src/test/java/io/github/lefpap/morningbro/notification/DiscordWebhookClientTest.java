package io.github.lefpap.morningbro.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// @RestClientTest doesn't include the HTTP service group auto-configuration (Boot 4.1), so it's imported here.
// The URL points at a closed local port, so a request that bypasses the mock fails instead of reaching Discord.
@RestClientTest(components = DiscordClientConfig.class, properties = "spring.http.serviceclient.discord.base-url=" + DiscordWebhookClientTest.URL)
@ImportAutoConfiguration({HttpServiceClientPropertiesAutoConfiguration.class, HttpServiceClientAutoConfiguration.class})
class DiscordWebhookClientTest {

    static final String URL = "http://127.0.0.1:9/api/webhooks/123/secret-token";

    @Autowired
    private DiscordWebhookClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void postsContentAsJson() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value("gm ☀️"))
                .andRespond(withNoContent());

        client.postText("gm ☀️");

        server.verify();
    }

    @Test
    void non2xxThrowsWithoutWebhookUrl() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Throwable thrown = catchThrowable(() -> client.postText("gm"));

        assertThat(thrown).isInstanceOf(DiscordWebhookException.class).hasNoCause();
        assertThat(thrown).hasMessageContaining("429");
        assertThat(thrown.toString()).doesNotContain("secret-token");
    }

    @Test
    void redirectThrowsInsteadOfPassingSilently() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.MOVED_PERMANENTLY));

        Throwable thrown = catchThrowable(() -> client.postText("gm"));

        assertThat(thrown).isInstanceOf(DiscordWebhookException.class).hasMessageContaining("301");
    }

    @Test
    void ioErrorDoesNotLeakWebhookUrl() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException()));

        Throwable thrown = catchThrowable(() -> client.postText("gm"));

        assertThat(thrown).isInstanceOf(DiscordWebhookException.class).hasNoCause();
        assertThat(thrown.toString()).doesNotContain("secret-token");
    }

}
