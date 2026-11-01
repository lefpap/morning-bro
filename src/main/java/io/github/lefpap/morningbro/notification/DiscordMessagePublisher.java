package io.github.lefpap.morningbro.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

// Exists only on a real run: a dry run has no Discord client (see DiscordClientConfig).
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
