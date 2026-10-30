package io.github.lefpap.morningbro.openmeteo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// @RestClientTest doesn't include the HTTP service group auto-configuration (Boot 4.1), so it's imported here.
// The URL points at a closed local port, so a request that bypasses the mock fails instead of reaching Open-Meteo.
@RestClientTest(components = OpenMeteoClientConfig.class, properties = "spring.http.serviceclient.openmeteo.base-url=" + OpenMeteoClientTest.BASE_URL)
@ImportAutoConfiguration({HttpServiceClientPropertiesAutoConfiguration.class, HttpServiceClientAutoConfiguration.class})
class OpenMeteoClientTest {

    static final String BASE_URL = "http://127.0.0.1:9";

    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    // A real response shape, including the fields the client ignores.
    private static final String FORECAST = """
            {"latitude":38.0,"longitude":23.75,"generationtime_ms":0.05,"utc_offset_seconds":7200,
             "timezone":"Europe/Athens","timezone_abbreviation":"EET","elevation":110.0,
             "daily_units":{"time":"iso8601","weather_code":"wmo code","temperature_2m_max":"°C",
                            "temperature_2m_min":"°C","precipitation_probability_max":"%"},
             "daily":{"time":["2026-10-30"],"weather_code":[3],"temperature_2m_max":[21.4],
                      "temperature_2m_min":[-0.4],"precipitation_probability_max":[25]}}
            """;

    @Autowired
    private OpenMeteoClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void requestsTodayForLocationInZone() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/v1/forecast?")))
                .andExpect(decodedQueryParam("latitude", "37.98"))
                .andExpect(decodedQueryParam("longitude", "-9.14"))
                .andExpect(decodedQueryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"))
                .andExpect(decodedQueryParam("timezone", "Europe/Athens"))
                .andExpect(decodedQueryParam("forecast_days", "1"))
                .andRespond(withSuccess(FORECAST, MediaType.APPLICATION_JSON));

        client.today(37.98, -9.14, ATHENS);

        server.verify();
    }

    @Test
    void parsesDailyForecast() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess(FORECAST, MediaType.APPLICATION_JSON));

        OpenMeteoForecast.Daily daily = client.today(37.98, 23.73, ATHENS).daily();

        assertThat(daily.time()).containsExactly(LocalDate.of(2026, 10, 30));
        assertThat(daily.weatherCode()).containsExactly(3);
        assertThat(daily.temperatureMax()).containsExactly(21.4);
        assertThat(daily.temperatureMin()).containsExactly(-0.4);
        assertThat(daily.precipitationProbabilityMax()).containsExactly(25);
    }

    @Test
    void parsesNullValues() {
        String body = """
                {"daily":{"time":["2026-10-30"],"weather_code":[null],"temperature_2m_max":[null],
                          "temperature_2m_min":[null],"precipitation_probability_max":[null]}}
                """;
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        OpenMeteoForecast.Daily daily = client.today(37.98, 23.73, ATHENS).daily();

        assertThat(daily.weatherCode()).containsExactly((Integer) null);
        assertThat(daily.temperatureMax()).containsExactly((Double) null);
        assertThat(daily.temperatureMin()).containsExactly((Double) null);
        assertThat(daily.precipitationProbabilityMax()).containsExactly((Integer) null);
    }

    @Test
    void parsesMissingDaily() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"latitude\":38.0,\"longitude\":23.75}", MediaType.APPLICATION_JSON));

        OpenMeteoForecast forecast = client.today(37.98, 23.73, ATHENS);

        assertThat(forecast.daily()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "INTERNAL_SERVER_ERROR"})
    void errorStatusThrowsWithReasonInMessage(HttpStatus status) {
        String body = "{\"error\":true,\"reason\":\"Latitude must be in range of -90 to 90°.\"}";
        server.expect(method(HttpMethod.GET))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));

        assertThatThrownBy(() -> client.today(37.98, 23.73, ATHENS))
                .isInstanceOf(OpenMeteoException.class)
                .hasMessageContaining(String.valueOf(status.value()))
                .hasMessageContaining("Latitude must be in range of -90 to 90°.");

        server.verify();
    }

    @Test
    void errorWithoutResponseBodyThrows() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.today(37.98, 23.73, ATHENS))
                .isInstanceOf(OpenMeteoException.class)
                .hasMessage("Open-Meteo request failed (503 SERVICE_UNAVAILABLE)");

        server.verify();
    }

    // @RequestParam values are encoded as URI variables, so '/' in the time zone arrives as %2F. Compare decoded values.
    private static RequestMatcher decodedQueryParam(String name, String expected) {
        return request -> {
            String raw = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(name);
            assertThat(raw).as("query param '%s'", name).isNotNull();
            assertThat(UriUtils.decode(raw, StandardCharsets.UTF_8)).as("query param '%s'", name).isEqualTo(expected);
        };
    }

}
