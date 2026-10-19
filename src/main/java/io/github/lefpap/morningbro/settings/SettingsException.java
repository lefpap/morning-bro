package io.github.lefpap.morningbro.settings;

/** A mistake in the Settings File or the environment variables. The message is written for the Owner. */
public class SettingsException extends RuntimeException {

    SettingsException(String message) {
        super(message);
    }
}
