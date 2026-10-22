package io.github.lefpap.morningbro.schedule;

import java.time.Instant;

/**
 * Decides whether a run posts: only within the Post Time hour, unless it is a Forced Post.
 */
public class SendWindow {

    private final ScheduleSettings schedule;
    private final boolean force;

    public SendWindow(ScheduleSettings schedule, boolean force) {
        this.schedule = schedule;
        this.force = force;
    }

    public boolean isOpen(Instant now) {
        return force || now.atZone(schedule.timezone()).getHour() == schedule.hour();
    }
}
