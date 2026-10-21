package io.github.lefpap.morningbro.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The app's rules for the forecasts, with the forecast source mocked.
 */
class WeatherServiceTest {

    private static final City ATHENS = new City("Athens", "🇬🇷", 37.98, 23.73);
    private static final City BRUSSELS = new City("Brussels", "🇧🇪", 50.85, 4.35);
    private static final List<City> CITIES = List.of(ATHENS, BRUSSELS);
    private static final LocalDate FORECAST_DAY = LocalDate.of(2026, 10, 21);

    private static final Forecast ATHENS_FORECAST = new Forecast(FORECAST_DAY, 45, 12.3, 22.4, OptionalInt.of(0));
    private static final Forecast BRUSSELS_FORECAST = new Forecast(FORECAST_DAY, 53, 8.8, 12.7, OptionalInt.of(51));

    private final OpenMeteoClient client = mock(OpenMeteoClient.class);
    private final WeatherService weatherService = new WeatherService(client);

    @Test
    void pairsEachCityWithItsForecastInOrder() {
        when(client.forecasts(CITIES, FORECAST_DAY)).thenReturn(List.of(ATHENS_FORECAST, BRUSSELS_FORECAST));

        assertThat(weatherService.forecasts(CITIES, FORECAST_DAY))
            .containsExactly(entry(ATHENS, ATHENS_FORECAST), entry(BRUSSELS, BRUSSELS_FORECAST));
    }

    @Test
    void failsWhenACityHasNoForecast() {
        when(client.forecasts(CITIES, FORECAST_DAY)).thenReturn(List.of(ATHENS_FORECAST));

        assertForecastsFail();
    }

    @Test
    void failsWhenThereAreMoreForecastsThanCities() {
        when(client.forecasts(CITIES, FORECAST_DAY))
            .thenReturn(List.of(ATHENS_FORECAST, BRUSSELS_FORECAST, BRUSSELS_FORECAST));

        assertForecastsFail();
    }

    @Test
    void failsWhenAForecastIsForAnotherDay() {
        Forecast tomorrow = new Forecast(FORECAST_DAY.plusDays(1), 53, 8.8, 12.7, OptionalInt.of(51));
        when(client.forecasts(CITIES, FORECAST_DAY)).thenReturn(List.of(ATHENS_FORECAST, tomorrow));

        assertForecastsFail();
    }

    private void assertForecastsFail() {
        assertThatThrownBy(() -> weatherService.forecasts(CITIES, FORECAST_DAY))
            .isInstanceOf(WeatherException.class);
    }
}
