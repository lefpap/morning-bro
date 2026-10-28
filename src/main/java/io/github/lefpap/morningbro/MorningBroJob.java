package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.discord.DiscordWebhookClient;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morning-bro.run-on-startup", havingValue = "true", matchIfMissing = true)
public class MorningBroJob implements ApplicationRunner {

    private final DiscordWebhookClient discord;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        discord.postText("Morning Bro is alive ✅");
    }

}
