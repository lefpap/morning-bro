package io.github.lefpap.morningbro.notification;

/**
 * The notification module's public API: delivers the morning message (to Discord, internally).
 * Not available on a dry run ({@code morning-bro.dry-run}), where the job logs the message instead.
 */
public interface MessagePublisher {

    /**
     * Delivers the message, throwing if delivery fails so the run fails.
     */
    void publish(String message);

}
