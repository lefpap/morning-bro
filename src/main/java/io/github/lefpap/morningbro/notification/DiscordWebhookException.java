package io.github.lefpap.morningbro.notification;

/**
 * A failed webhook post; its message never contains the webhook URL.
 */
class DiscordWebhookException extends RuntimeException {

    DiscordWebhookException(String message) {
        super(message);
    }

}
