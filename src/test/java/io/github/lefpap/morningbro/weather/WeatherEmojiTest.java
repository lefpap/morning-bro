package io.github.lefpap.morningbro.weather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class WeatherEmojiTest {

    // Asserted by code point because the variation selector (U+FE0F) is invisible.
    @Test
    void constantsHaveExactCodePoints() {
        assertThat(WeatherEmoji.SUN.codePoints().toArray()).containsExactly(0x2600, 0xFE0F);
        assertThat(WeatherEmoji.PARTLY_CLOUDY.codePoints().toArray()).containsExactly(0x26C5);
        assertThat(WeatherEmoji.CLOUD.codePoints().toArray()).containsExactly(0x2601, 0xFE0F);
        assertThat(WeatherEmoji.FOG.codePoints().toArray()).containsExactly(0x1F32B, 0xFE0F);
        assertThat(WeatherEmoji.SHOWERS.codePoints().toArray()).containsExactly(0x1F326, 0xFE0F);
        assertThat(WeatherEmoji.RAIN.codePoints().toArray()).containsExactly(0x1F327, 0xFE0F);
        assertThat(WeatherEmoji.SNOW.codePoints().toArray()).containsExactly(0x1F328, 0xFE0F);
        assertThat(WeatherEmoji.THUNDERSTORM.codePoints().toArray()).containsExactly(0x26C8, 0xFE0F);
        assertThat(WeatherEmoji.UNKNOWN.codePoints().toArray()).containsExactly(0x1F321, 0xFE0F);
    }

    @ParameterizedTest
    @MethodSource
    void mapsCodes(int code, String emoji) {
        assertThat(WeatherEmoji.forCode(code)).isEqualTo(emoji);
    }

    // Every range boundary.
    static Stream<Arguments> mapsCodes() {
        return Stream.of(
            arguments(0, WeatherEmoji.SUN),
            arguments(1, WeatherEmoji.PARTLY_CLOUDY), arguments(2, WeatherEmoji.PARTLY_CLOUDY),
            arguments(3, WeatherEmoji.CLOUD),
            arguments(45, WeatherEmoji.FOG), arguments(48, WeatherEmoji.FOG),
            arguments(51, WeatherEmoji.SHOWERS), arguments(57, WeatherEmoji.SHOWERS),
            arguments(80, WeatherEmoji.SHOWERS), arguments(82, WeatherEmoji.SHOWERS),
            arguments(61, WeatherEmoji.RAIN), arguments(67, WeatherEmoji.RAIN),
            arguments(71, WeatherEmoji.SNOW), arguments(77, WeatherEmoji.SNOW),
            arguments(85, WeatherEmoji.SNOW), arguments(86, WeatherEmoji.SNOW),
            arguments(95, WeatherEmoji.THUNDERSTORM), arguments(99, WeatherEmoji.THUNDERSTORM));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, 44, 46, 47, 49, 50, 58, 60, 68, 70, 78, 79, 83, 84, 87, 94, 100})
    void unknownCodesMapToThermometer(int code) {
        assertThat(WeatherEmoji.forCode(code)).isEqualTo(WeatherEmoji.UNKNOWN);
    }

}
