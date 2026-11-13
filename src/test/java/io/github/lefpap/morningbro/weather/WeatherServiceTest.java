package io.github.lefpap.morningbro.weather;

import io.github.lefpap.morningbro.MorningBroProperties;
import io.github.lefpap.morningbro.MorningBroProperties.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Athens");
    private static final Location ATHENS = new Location("Athens", "GR", 37.98, 23.73);
    private static final MorningBroProperties PROPERTIES = new MorningBroProperties(
        ZONE, "Athens", new MorningBroProperties.Message("Good Morning", "Take it easy Friday!", "-# Weather by Open-Meteo"), List.of(ATHENS), false);

    private static final OpenMeteoForecast CLOUDY_DAY = new OpenMeteoForecast(new OpenMeteoForecast.Daily(
        List.of(LocalDate.of(2026, 10, 30)), List.of(3), List.of(21.4), List.of(15.2), List.of(25)));

    private record Request(double latitude, double longitude, ZoneId zone) {
    }

    @Test
    void requestsTodayForLocationInConfiguredZone() {
        List<Request> requests = new ArrayList<>();
        WeatherService service = serviceWith((latitude, longitude, zone) -> {
            requests.add(new Request(latitude, longitude, zone));
            return CLOUDY_DAY;
        });

        Optional<Forecast> forecast = service.today(ATHENS);

        assertThat(requests).containsExactly(new Request(37.98, 23.73, ZONE));
        assertThat(forecast).contains(new Forecast(WeatherEmoji.CLOUD, 15, 21, 25));
    }

    @Test
    void missingDataIsNoForecast() {
        WeatherService service = serviceWith((_, _, _) -> new OpenMeteoForecast(null));

        assertThat(service.today(ATHENS)).isEmpty();
    }

    @Test
    void clientFailurePropagates() {
        OpenMeteoException failure = new OpenMeteoException("Open-Meteo request failed (500 INTERNAL_SERVER_ERROR)");
        WeatherService service = serviceWith((_, _, _) -> {
            throw failure;
        });

        assertThatThrownBy(() -> service.today(ATHENS)).isSameAs(failure);
    }

    private static WeatherService serviceWith(OpenMeteoClient client) {
        return new WeatherService(client, PROPERTIES);
    }

}
