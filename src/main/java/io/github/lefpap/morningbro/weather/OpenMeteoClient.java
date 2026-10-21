package io.github.lefpap.morningbro.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * The only class that knows Open-Meteo: the request, the response records, and their mapping to Forecasts.
 */
public class OpenMeteoClient {

    private static final URI OPEN_METEO = URI.create("https://api.open-meteo.com");

    private static final String DAILY_FIELDS =
        "weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max";

    /**
     * One location comes back as an object, several as an array. Values must have the type Open-Meteo documents:
     * a number sent as text, or a decimal where a whole number belongs, is unexpected data, not something to convert.
     */
    private static final JsonMapper MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .build();

    /**
     * How long to wait for Open-Meteo before giving up, so a hung connection can't stall the morning run.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final URI baseUrl;

    public OpenMeteoClient() {
        this(OPEN_METEO);
    }

    /**
     * @param baseUrl where Open-Meteo is served. Tests point it at a local server.
     */
    public OpenMeteoClient(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * The forecasts for these cities from the Forecast Day, in the order Open-Meteo returns them.
     */
    public List<Forecast> forecasts(List<City> cities, LocalDate forecastDay) {
        URI uri = uriFor(cities, forecastDay);
        List<Location> locations = fetchLocations(uri);
        return locations.stream().map(Location::forecast).toList();
    }

    /**
     * Sends the request and reads the Locations in the response.
     */
    private List<Location> fetchLocations(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WeatherException("Open-Meteo answered HTTP %d: %s"
                    .formatted(response.statusCode(), response.body()));
            }
            return parse(response.body());
        } catch (IOException e) {
            // HttpClient often leaves the message empty, so the exception's name says what went wrong.
            throw new WeatherException("Cannot reach Open-Meteo: " + e);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new WeatherException("Interrupted while fetching the forecast");
        }
    }

    private static List<Location> parse(String body) {
        try {
            return MAPPER.readValue(body, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new WeatherException("Open-Meteo sent a response that can't be read: " + e.getOriginalMessage());
        }
    }

    private URI uriFor(List<City> cities, LocalDate forecastDay) {
        return baseUrl.resolve("/v1/forecast"
            + "?latitude=" + join(cities, City::lat)
            + "&longitude=" + join(cities, City::lon)
            + "&daily=" + DAILY_FIELDS
            + "&timezone=auto"
            + "&start_date=" + forecastDay
            + "&end_date=" + forecastDay);
    }

    private static String join(List<City> cities, ToDoubleFunction<City> coordinate) {
        return cities.stream()
            .map(city -> String.valueOf(coordinate.applyAsDouble(city)))
            .collect(Collectors.joining(","));
    }

    /**
     * One location's part of the response. Fields we don't use are ignored.
     */
    record Location(Daily daily) {

        Forecast forecast() {
            if (daily == null) {
                throw missing("daily");
            }
            Integer rainChance = first("precipitation_probability_max", daily.precipitationProbabilityMax());
            return new Forecast(
                required("time", daily.time()),
                required("weather_code", daily.weatherCode()),
                required("temperature_2m_min", daily.temperatureMin()),
                required("temperature_2m_max", daily.temperatureMax()),
                rainChance == null ? OptionalInt.empty() : OptionalInt.of(rainChance));
        }

        private static <T> T required(String field, List<T> values) {
            T value = first(field, values);
            if (value == null) {
                throw missing(field);
            }
            return value;
        }

        /**
         * The one day's value, which may be null. The field must be there, with one value for the one day.
         */
        private static <T> T first(String field, List<T> values) {
            if (values == null) {
                throw missing(field);
            }
            if (values.size() != 1) {
                throw new WeatherException("Open-Meteo's forecast has %d values for %s, not 1"
                    .formatted(values.size(), field));
            }
            return values.getFirst();
        }

        private static WeatherException missing(String field) {
            return new WeatherException("Open-Meteo's forecast has no " + field);
        }
    }

    /**
     * The daily fields requested in {@link #DAILY_FIELDS}, one entry per day.
     */
    record Daily(
        List<LocalDate> time,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
        @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
        @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax) {
    }
}
