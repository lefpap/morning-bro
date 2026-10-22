package io.github.lefpap.morningbro.messaging.discord;

/** The Morning Message couldn't be posted to the Channel. The message is written for the Owner. */
public class DiscordException extends RuntimeException {

    DiscordException(String message) {
        super(message);
    }
}
