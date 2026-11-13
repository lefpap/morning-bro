package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.MorningBroProperties.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers only the rules this project adds; the standard constraint annotations are Spring's and Bean Validation's job.
 */
class MorningBroPropertiesTest {

    private static final Location ATHENS = new Location("Athens", "GR", 37.98, 23.73);
    private static final Location BRUSSELS = new Location("Brussels", "BE", 50.85, 4.35);

    // Exact, case-sensitive match against location names.
    @ParameterizedTest
    @ValueSource(strings = {"athens", "ATHENS", "Athen", "Milan"})
    void unknownHomeIsRejected(String home) {
        assertThat(properties(home, List.of(ATHENS, BRUSSELS)).isHomeKnown()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Athens", "Brussels"})
    void homeMayBeAnyLocation(String home) {
        assertThat(properties(home, List.of(ATHENS, BRUSSELS)).isHomeKnown()).isTrue();
    }

    @Test
    void duplicateLocationNamesAreRejected() {
        Location otherAthens = new Location("Athens", "US", 33.96, -83.38);

        assertThat(properties("Athens", List.of(ATHENS, otherAthens)).isLocationNamesUnique()).isFalse();
    }

    @Test
    void distinctLocationNamesAreAccepted() {
        assertThat(properties("Athens", List.of(ATHENS, BRUSSELS)).isLocationNamesUnique()).isTrue();
    }

    private static MorningBroProperties properties(String home, List<Location> locations) {
        return new MorningBroProperties(
            ZoneId.of("Europe/Athens"),
            home,
            new MorningBroProperties.Message("Good Morning", "Take it easy Friday!", "-# Weather by Open-Meteo"),
            locations,
            false);
    }

}
