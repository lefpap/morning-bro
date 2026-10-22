package io.github.lefpap.morningbro.messaging.discord;

import java.util.Optional;

public record DiscordSettings(
        String username,
        Optional<String> avatarUrl,
        Optional<String> webhookUrl) {
}
