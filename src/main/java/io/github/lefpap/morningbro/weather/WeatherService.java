package io.github.lefpap.morningbro.weather;

import io.github.lefpap.morningbro.MorningBroProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenMeteoClient openMeteo;
    private final MorningBroProperties properties;

    // Today's forecast in the configured zone, or empty when Open-Meteo has no data for it.
    public Optional<Forecast> today(MorningBroProperties.Location location) {
        OpenMeteoForecast response = openMeteo.today(location.latitude(), location.longitude(), properties.timeZone());
        return ForecastConverter.convert(response);
    }

}
