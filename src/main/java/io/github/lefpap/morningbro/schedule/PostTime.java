package io.github.lefpap.morningbro.schedule;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The hour of the day, in the Reference Timezone, at which the Scheduled Post goes out.
 * A run is judged by the slot it was scheduled for, not by when it started, so a late run
 * neither posts a second time nor misses the day.
 */
public record PostTime(int hour, ZoneId timezone) {

    /** A daily cron entry, "M H * * *", in UTC: one per hour in the workflow. */
    private static final Pattern DAILY_CRON = Pattern.compile("(\\d{1,2})\\s+(\\d{1,2})\\s+\\*\\s+\\*\\s+\\*");

    /**
     * The instant the run was scheduled for: the latest time the cron entry matches at or before now.
     * Without a cron entry, as in a Forced Post or a local run, it is now.
     */
    public static Instant scheduledFor(Optional<String> scheduledCron, Instant now) {
        if (scheduledCron.isEmpty()) {
            return now;
        }
        String cron = scheduledCron.get();
        Matcher matcher = DAILY_CRON.matcher(cron.strip());
        if (!matcher.matches()) {
            throw malformed(cron);
        }
        int minute = Integer.parseInt(matcher.group(1));
        int hour = Integer.parseInt(matcher.group(2));
        if (minute > 59 || hour > 23) {
            throw malformed(cron);
        }

        ZonedDateTime utcNow = now.atZone(ZoneOffset.UTC);
        ZonedDateTime slot = utcNow.truncatedTo(ChronoUnit.DAYS).withHour(hour).withMinute(minute);
        return (slot.isAfter(utcNow) ? slot.minusDays(1) : slot).toInstant();
    }

    /**
     * Whether a run scheduled for this instant posts: it is within the hour from the Post Time on its Forecast Day.
     * In an hour the clock repeats, that is the first one; in an hour it skips, the hour after the gap.
     */
    public boolean isDueAt(Instant scheduledFor) {
        Instant postInstant = ZonedDateTime.of(forecastDay(scheduledFor).atTime(hour, 0), timezone).toInstant();
        return !scheduledFor.isBefore(postInstant) && scheduledFor.isBefore(postInstant.plus(Duration.ofHours(1)));
    }

    public LocalDate forecastDay(Instant scheduledFor) {
        return LocalDate.ofInstant(scheduledFor, timezone);
    }

    private static ScheduleException malformed(String cron) {
        return new ScheduleException(
            "SCHEDULED_CRON must be a daily cron entry like \"37 5 * * *\", but is \"%s\"".formatted(cron));
    }
}
