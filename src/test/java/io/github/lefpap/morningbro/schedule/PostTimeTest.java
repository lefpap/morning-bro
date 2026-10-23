package io.github.lefpap.morningbro.schedule;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PostTimeTest {

    /** The Post Time is 08:00 in Athens, which is UTC+2 in winter and UTC+3 in summer. */
    private final PostTime postTime = new PostTime(8, ZoneId.of("Europe/Athens"));

    /** 08:00, 08:07 and 08:59 Athens time in winter. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T06:00:00Z", "2026-01-15T06:07:00Z", "2026-01-15T06:59:59Z"})
    void aRunWithinThePostTimeHourProceeds(String now) {
        assertThat(postTime.isDueAt(Instant.parse(now))).isTrue();
    }

    /** 07:59 and 09:00 Athens time in winter, 08:00 UTC, and 08:00 the evening before. */
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-15T05:59:59Z", "2026-01-15T07:00:00Z", "2026-01-15T08:00:00Z", "2026-01-14T18:00:00Z"})
    void anyOtherHourDoesNothing(String now) {
        assertThat(postTime.isDueAt(Instant.parse(now))).isFalse();
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
    void thePostTimeStaysAtTheSameLocalHourAcrossDaylightSavingChanges(String now, boolean open) {
        assertThat(postTime.isDueAt(Instant.parse(now))).isEqualTo(open);
    }

}
