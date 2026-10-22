import io.github.lefpap.morningbro.greeting.MessagePublisher;
import io.github.lefpap.morningbro.greeting.MorningMessage;
import io.github.lefpap.morningbro.greeting.MorningMessageFactory;
import io.github.lefpap.morningbro.greeting.console.ConsoleComposer;
import io.github.lefpap.morningbro.greeting.console.ConsolePublisher;
import io.github.lefpap.morningbro.settings.Settings;
import io.github.lefpap.morningbro.settings.SettingsException;
import io.github.lefpap.morningbro.weather.*;

void main() {
    try {
        Settings settings = Settings.load(System.getenv());
        run(settings);
    } catch (SettingsException e) {
        exit(e.getMessage());
    }
}

void run(Settings settings) {
    OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    WeatherService weatherService = new WeatherService(openMeteoClient);
    MorningMessageFactory messageFactory = new MorningMessageFactory(settings.greeting(), settings.weather());
    MessagePublisher messagePublisher = new ConsolePublisher(new ConsoleComposer());

    IO.println(settings.summary());

    LocalDate forecastDay = LocalDate.now(settings.schedule().timezone());
    IO.println("Forecast Day: " + forecastDay);
    Map<City, Forecast> forecastsByCity = fetchForecastsByCity(weatherService, settings.cities(), forecastDay);
    MorningMessage message = messageFactory.create(forecastsByCity, forecastDay);
    IO.println();
    messagePublisher.publish(message);
}

/**
 * The forecasts, telling the Owner in the run log why the weather is unavailable when it is.
 */
Map<City, Forecast> fetchForecastsByCity(WeatherService weatherService, List<City> cities, LocalDate forecastDay) {
    try {
        return weatherService.forecasts(cities, forecastDay);
    } catch (WeatherException e) {
        System.err.println("The weather is unavailable, so the Fallback Message goes out: " + e.getMessage());
        return Map.of();
    }
}

void exit(String message) {
    System.err.println(message);
    System.exit(1);
}
