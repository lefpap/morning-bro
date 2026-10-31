package io.github.lefpap.morningbro.weather;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

class ForecastConverterTest {

    private static final List<LocalDate> TODAY = List.of(LocalDate.of(2026, 10, 30));

    @Test
    void convertsFirstDay() {
        assertThat(ForecastConverter.convert(daily(3, 21.4, 15.2, 25)))
            .contains(new Forecast(WeatherEmoji.CLOUD, 15, 21, 25));
    }

    // Ties away from zero; an int result can't be negative zero.
    @ParameterizedTest
    @CsvSource({"-0.4, 0", "-3.6, -4", "-2.5, -3", "-0.5, -1", "2.5, 3", "0.4, 0", "14.49, 14"})
    void roundsHalfUp(double temperature, int expected) {
        assertThat(ForecastConverter.convert(daily(0, 30.0, temperature, 0)))
            .hasValueSatisfying(forecast -> assertThat(forecast.minTemperature()).isEqualTo(expected));
    }

    // One missing value means no forecast at all, never a partial one.
    @ParameterizedTest
    @MethodSource
    void incompleteDataIsNoForecast(OpenMeteoForecast response) {
        assertThat(ForecastConverter.convert(response)).isEmpty();
    }

    static Stream<Named<OpenMeteoForecast>> incompleteDataIsNoForecast() {
        List<Integer> code = List.of(3);
        List<Double> max = List.of(21.4);
        List<Double> min = List.of(15.2);
        List<Integer> rain = List.of(25);
        return Stream.of(
            named("no response", null),
            named("no daily data", new OpenMeteoForecast(null)),
            named("no weather codes", daily(null, max, min, rain)),
            named("no max temperatures", daily(code, null, min, rain)),
            named("no min temperatures", daily(code, max, null, rain)),
            named("no rain chances", daily(code, max, min, null)),
            named("empty weather codes", daily(List.of(), max, min, rain)),
            named("empty max temperatures", daily(code, List.of(), min, rain)),
            named("empty min temperatures", daily(code, max, List.of(), rain)),
            named("empty rain chances", daily(code, max, min, List.of())),
            named("null weather code", daily(null, 21.4, 15.2, 25)),
            named("null max temperature", daily(3, null, 15.2, 25)),
            named("null min temperature", daily(3, 21.4, null, 25)),
            named("null rain chance", daily(3, 21.4, 15.2, null)));
    }

    // One day of values; singletonList keeps a null value (List.of rejects it).
    private static OpenMeteoForecast daily(Integer code, Double max, Double min, Integer rain) {
        return daily(
            Collections.singletonList(code),
            Collections.singletonList(max),
            Collections.singletonList(min),
            Collections.singletonList(rain)
        );
    }

    private static OpenMeteoForecast daily(List<Integer> code, List<Double> max, List<Double> min, List<Integer> rain) {
        return new OpenMeteoForecast(new OpenMeteoForecast.Daily(TODAY, code, max, min, rain));
    }

}
