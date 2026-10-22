package io.github.lefpap.morningbro.greeting.console;

import io.github.lefpap.morningbro.greeting.MessagePublisher;
import io.github.lefpap.morningbro.greeting.MorningMessage;

/** Prints the Morning Message to the run log as plain text, so the Owner can check it. */
public class ConsolePublisher implements MessagePublisher {

    private final ConsoleComposer composer;

    public ConsolePublisher(ConsoleComposer composer) {
        this.composer = composer;
    }

    @Override
    public void publish(MorningMessage message) {
        IO.println(composer.compose(message));
    }
}
