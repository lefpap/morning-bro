package io.github.lefpap.morningbro.notification;

/**
 * The JSON body of a webhook post; {@code content} is the message text.
 */
record DiscordMessage(String content) {
}
