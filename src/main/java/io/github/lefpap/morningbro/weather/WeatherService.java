package io.github.lefpap.morningbro.weather;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One Forecast per city for the Forecast Day, checked against the app's rules.
 */
public class WeatherService {

    private final OpenMeteoClient client;

    public WeatherService(OpenMeteoClient client) {
        this.client = client;
    }

    /**
     * One Forecast per city, in the order of the cities. Partial or unexpected forecasts are an error.
     */
    public Map<City, Forecast> forecasts(List<City> cities, LocalDate forecastDay) {
        List<Forecast> forecasts = client.forecasts(cities, forecastDay);
        if (forecasts.size() != cities.size()) {
            throw new WeatherException("Got %d forecast(s) for %d cities".formatted(forecasts.size(), cities.size()));
        }
        Map<City, Forecast> byCity = new LinkedHashMap<>();
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            Forecast forecast = forecasts.get(i);
            if (!forecast.day().equals(forecastDay)) {
                throw new WeatherException("The forecast for %s is for %s, not the Forecast Day %s"
                    .formatted(city.name(), forecast.day(), forecastDay));
            }
            byCity.put(city, forecast);
        }
        return byCity;
    }
}
