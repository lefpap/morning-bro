package io.github.lefpap.morningbro.weather;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a local server that answers like Open-Meteo, with responses from src/test/resources/open-meteo/.
 * No calls leave the machine.
 */
class OpenMeteoClientTest {

    private static final City ATHENS = new City("Athens", "🇬🇷", 37.98, 23.73);
    private static final City BRUSSELS = new City("Brussels", "🇧🇪", 50.85, 4.35);
    private static final LocalDate FORECAST_DAY = LocalDate.of(2026, 10, 21);

    private final List<URI> requests = new CopyOnWriteArrayList<>();
    private HttpServer openMeteo;
    private OpenMeteoClient client;

    @BeforeEach
    void startOpenMeteo() throws IOException {
        openMeteo = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        openMeteo.start();
        InetSocketAddress address = openMeteo.getAddress();
        client = new OpenMeteoClient(URI.create("http://" + address.getHostString() + ":" + address.getPort()));
    }

    @AfterEach
    void stopOpenMeteo() {
        openMeteo.stop(0);
    }

    @Test
    void fetchesTheForecastDayForEveryCityInOneRequest() {
        respondWith(200, "athens-brussels.json");

        List<Forecast> forecasts = client.forecasts(List.of(ATHENS, BRUSSELS), FORECAST_DAY);

        assertThat(forecasts).containsExactly(
            new Forecast(FORECAST_DAY, 45, 12.3, 22.4, OptionalInt.of(0)),
            new Forecast(FORECAST_DAY, 53, 8.8, 12.7, OptionalInt.of(51)));
        URI request = assertThat(requests).singleElement().actual();
        assertThat(request.getPath()).isEqualTo("/v1/forecast");
        assertThat(parametersOf(request)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "latitude", "37.98,50.85",
            "longitude", "23.73,4.35",
            "daily", "weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max",
            "timezone", "auto",
            "start_date", "2026-10-21",
            "end_date", "2026-10-21"));
    }

    @Test
    void readsTheSingleLocationOpenMeteoReturnsForOneCity() {
        respondWith(200, "athens.json");

        assertThat(client.forecasts(List.of(ATHENS), FORECAST_DAY))
            .containsExactly(new Forecast(FORECAST_DAY, 45, 12.3, 22.6, OptionalInt.of(0)));
    }

    @Test
    void readsAMissingRainChanceAsNone() {
        respondWith(200, "athens-no-rain-chance.json");

        assertThat(client.forecasts(List.of(ATHENS), FORECAST_DAY))
            .containsExactly(new Forecast(FORECAST_DAY, 45, 12.3, 22.6, OptionalInt.empty()));
    }

    @Test
    void readsAWholeNumberTemperature() {
        respondWith(200, "athens-whole-number-high.json");

        assertThat(client.forecasts(List.of(ATHENS), FORECAST_DAY))
            .containsExactly(new Forecast(FORECAST_DAY, 45, 12.3, 23.0, OptionalInt.of(0)));
    }

    /** Each file is athens.json with one thing broken, as its name says. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "no-daily.json",
        "no-temperature-max.json",
        "weather-code-null.json",
        "temperature-min-null.json",
        "temperature-max-null.json",
        "weather-code-empty.json",
        "weather-code-two-days.json",
        "weather-code-text.json",
        "temperature-as-text.json",
        "rain-chance-decimal.json",
        "not-json.html"})
    void failsOnABrokenResponse(String file) {
        respondWith(200, "broken/" + file);

        assertFetchFails();
    }

    @Test
    void failsWhenOpenMeteoAnswersWithAnError() {
        respondWith(400, "error-400.json");

        assertFetchFails();
    }

    @Test
    void failsWhenOpenMeteoCannotBeReached() {
        openMeteo.stop(0);

        assertFetchFails();
    }

    private void assertFetchFails() {
        assertThatThrownBy(() -> client.forecasts(List.of(ATHENS), FORECAST_DAY))
            .isInstanceOf(WeatherException.class);
    }

    /** Answers every request with this status and a file from src/test/resources/open-meteo/, and remembers the request. */
    private void respondWith(int status, String file) {
        byte[] body = read("/open-meteo/" + file);
        openMeteo.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI());
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    private static byte[] read(String resource) {
        try (InputStream in = OpenMeteoClientTest.class.getResourceAsStream(resource)) {
            assertThat(in).isNotNull();
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> parametersOf(URI uri) {
        return Arrays.stream(uri.getQuery().split("&"))
            .map(parameter -> parameter.split("=", 2))
            .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    }
}
