package io.github.lefpap.morningbro.schedule;

/** The run can't tell which slot it was scheduled for. The message is written for the Owner. */
public class ScheduleException extends RuntimeException {

    ScheduleException(String message) {
        super(message);
    }
}
