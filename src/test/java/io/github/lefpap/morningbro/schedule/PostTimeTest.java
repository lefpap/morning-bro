package io.github.lefpap.morningbro.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostTimeTest {

    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    /** The Post Time is 08:00 in Athens, which is UTC+2 in winter and UTC+3 in summer. */
    private final PostTime postTime = new PostTime(8, ATHENS);

    /** 08:00, 08:07 and 08:59 Athens time in winter. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T06:00:00Z", "2026-01-15T06:07:00Z", "2026-01-15T06:59:59Z"})
    void isDueWithinThePostTimeHour(String scheduledFor) {
        assertThat(postTime.isDueAt(Instant.parse(scheduledFor))).isTrue();
    }

    /** 07:59 and 09:00 Athens time in winter, 08:00 UTC, and 08:00 the evening before. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T05:59:59Z", "2026-01-15T07:00:00Z", "2026-01-15T08:00:00Z", "2026-01-14T18:00:00Z"})
    void isNotDueAtAnyOtherHour(String scheduledFor) {
        assertThat(postTime.isDueAt(Instant.parse(scheduledFor))).isFalse();
    }

    /**
     * Athens moves to summer time on 2026-03-29 and back on 2026-10-25, so 08:00 is 05:00 UTC
     * in summer and 06:00 UTC in winter, on the day of each change too.
     */
    @ParameterizedTest
    @CsvSource({
            "2026-03-29T05:00:00Z, true", "2026-03-29T06:00:00Z, false",
            "2026-07-15T05:00:00Z, true", "2026-07-15T06:00:00Z, false",
            "2026-10-25T06:00:00Z, true", "2026-10-25T05:00:00Z, false"})
    void staysAtTheSameLocalHourAcrossDaylightSavingChanges(String scheduledFor, boolean due) {
        assertThat(postTime.isDueAt(Instant.parse(scheduledFor))).isEqualTo(due);
    }

    /** The 05:37 UTC slot is 07:37 in Athens in winter, the hour before the Post Time. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T05:40:00Z", "2026-01-15T06:00:00Z", "2026-01-15T06:30:00Z", "2026-01-15T07:10:00Z"})
    void aRunScheduledForTheHourBeforeIsNotDueHoweverLateItStarts(String now) {
        Instant scheduledFor = PostTime.scheduledFor(Optional.of("37 5 * * *"), Instant.parse(now));

        assertThat(postTime.isDueAt(scheduledFor)).isFalse();
    }

    /** The 06:37 UTC slot is 08:37 in Athens in winter, within the Post Time hour. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T06:37:00Z", "2026-01-15T07:45:00Z", "2026-01-15T10:00:00Z"})
    void aRunScheduledForThePostTimeIsDueEvenIfItStartsLate(String now) {
        Instant scheduledFor = PostTime.scheduledFor(Optional.of("37 6 * * *"), Instant.parse(now));

        assertThat(scheduledFor).isEqualTo(Instant.parse("2026-01-15T06:37:00Z"));
        assertThat(postTime.isDueAt(scheduledFor)).isTrue();
        assertThat(postTime.forecastDay(scheduledFor)).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    /** A Post Time of 23:00 in Athens is the 21:37 UTC slot in winter; this run starts after midnight in Athens. */
    @Test
    void aLateRunUsesItsSlotsForecastDay() {
        PostTime lateEvening = new PostTime(23, ATHENS);

        Instant scheduledFor = PostTime.scheduledFor(Optional.of("37 21 * * *"), Instant.parse("2026-01-14T22:05:00Z"));

        assertThat(lateEvening.isDueAt(scheduledFor)).isTrue();
        assertThat(lateEvening.forecastDay(scheduledFor)).isEqualTo(LocalDate.of(2026, 1, 14));
    }

    @Test
    void theSlotIsOnThePreviousUtcDayWhenTheRunStartsAfterMidnightUtc() {
        assertThat(PostTime.scheduledFor(Optional.of("37 23 * * *"), Instant.parse("2026-01-15T00:20:00Z")))
                .isEqualTo(Instant.parse("2026-01-14T23:37:00Z"));
    }

    @Test
    void withoutAScheduledCronTheSlotIsNow() {
        Instant now = Instant.parse("2026-01-15T03:12:45Z");

        assertThat(PostTime.scheduledFor(Optional.empty(), now)).isEqualTo(now);
        assertThat(postTime.forecastDay(now)).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    /**
     * On 2026-10-25 Athens repeats 03:00–03:59: first at 00:xx UTC (summer time), then at 01:xx UTC (winter time).
     * Only the first counts.
     */
    @ParameterizedTest
    @CsvSource({"37 0 * * *, true", "37 1 * * *, false"})
    void aPostTimeInTheRepeatedHourIsDueOnce(String cron, boolean due) {
        PostTime threeAm = new PostTime(3, ATHENS);

        Instant scheduledFor = PostTime.scheduledFor(Optional.of(cron), Instant.parse("2026-10-25T02:00:00Z"));

        assertThat(threeAm.isDueAt(scheduledFor)).isEqualTo(due);
    }

    /**
     * On 2026-03-29 Athens skips 03:00–03:59: 01:00 UTC is 04:00 summer time.
     * The Post Time moves to the hour after the gap, so that day still posts.
     */
    @ParameterizedTest
    @CsvSource({"37 0 * * *, false", "37 1 * * *, true", "37 2 * * *, false"})
    void aPostTimeInTheSkippedHourIsStillDue(String cron, boolean due) {
        PostTime threeAm = new PostTime(3, ATHENS);

        Instant scheduledFor = PostTime.scheduledFor(Optional.of(cron), Instant.parse("2026-03-29T03:00:00Z"));

        assertThat(threeAm.isDueAt(scheduledFor)).isEqualTo(due);
    }

    @ParameterizedTest
    @ValueSource(strings = {"37 5", "37 5 * * 1", "*/5 * * * *", "x 5 * * *", "60 5 * * *", "37 24 * * *", "-1 5 * * *"})
    void aMalformedScheduledCronFails(String cron) {
        assertThatThrownBy(() -> PostTime.scheduledFor(Optional.of(cron), Instant.parse("2026-01-15T06:00:00Z")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("SCHEDULED_CRON must be a daily cron entry like \"37 5 * * *\", but is \"%s\"".formatted(cron));
    }
}
