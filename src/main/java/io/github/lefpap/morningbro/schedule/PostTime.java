package io.github.lefpap.morningbro.schedule;

import java.time.Instant;
import java.time.ZoneId;

/**
 * The hour of the day, in the Reference Timezone, at which the Scheduled Post goes out.
 */
public record PostTime(int hour, ZoneId timezone) {

    public boolean isDueAt(Instant now) {
        return now.atZone(timezone).getHour() == hour;
    }
}
