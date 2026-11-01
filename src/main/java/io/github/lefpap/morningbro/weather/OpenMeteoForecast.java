package io.github.lefpap.morningbro.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * The slice of Open-Meteo's forecast response that Morning Bro uses.
 */
record OpenMeteoForecast(Daily daily) {

    record Daily(
            List<LocalDate> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
            @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax) {
    }

}
