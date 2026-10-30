package io.github.lefpap.morningbro.openmeteo;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.time.ZoneId;

@HttpExchange
public interface OpenMeteoClient {

    // Must match the fields of OpenMeteoForecast.Daily.
    String DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max";

    @GetExchange("/v1/forecast?daily=" + DAILY_FIELDS + "&forecast_days=1")
    OpenMeteoForecast today(@RequestParam double latitude,
                            @RequestParam double longitude,
                            @RequestParam("timezone") ZoneId zone);
}
