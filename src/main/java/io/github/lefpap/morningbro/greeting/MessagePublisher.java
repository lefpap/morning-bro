package io.github.lefpap.morningbro.greeting;

/**
 * Delivers a Morning Message on one platform. Each implementation shows it in that platform's own structure and format,
 * and delivers it its own way: the console prints it, Discord posts it to the Channel.
 */
public interface MessagePublisher {

    void publish(MorningMessage message);
}
