package io.github.lefpap.morningbro.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryFlagTest {

    @ParameterizedTest
    @CsvSource({"GR, 1F1EC, 1F1F7", "BE, 1F1E7, 1F1EA", "IT, 1F1EE, 1F1F9", "AZ, 1F1E6, 1F1FF"})
    void mapsLettersToRegionalIndicators(String countryCode, String first, String second) {
        assertThat(CountryFlag.of(countryCode).codePoints().toArray())
            .containsExactly(Integer.parseInt(first, 16), Integer.parseInt(second, 16));
    }

    @Test
    void greeceIsTheGreekFlag() {
        assertThat(CountryFlag.of("GR")).isEqualTo("🇬🇷");
    }

}
