package io.github.lefpap.morningbro.messaging.console;

import io.github.lefpap.morningbro.messaging.MessagePublisher;
import io.github.lefpap.morningbro.messaging.MorningMessage;

/** Prints the Morning Message to the run log as plain text, so the Owner can check it. */
public class ConsoleMessagePublisher implements MessagePublisher {

    private final ConsoleMessageComposer composer;

    public ConsoleMessagePublisher(ConsoleMessageComposer composer) {
        this.composer = composer;
    }

    @Override
    public void publish(MorningMessage message) {
        IO.println(composer.compose(message));
    }
}
