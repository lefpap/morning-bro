package io.github.lefpap.morningbro.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Posts the morning message to the Discord webhook. It exists only on a real run, because a dry run has no
 * Discord client (see {@link DiscordClientConfig}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(name = "morning-bro.dry-run", havingValue = false, matchIfMissing = true)
class DiscordMessagePublisher implements MessagePublisher {

    private final DiscordWebhookClient client;

    @Override
    public void publish(String message) {
        log.info("Posting morning message:\n{}", message);
        client.post(new DiscordMessage(message));
    }

}
