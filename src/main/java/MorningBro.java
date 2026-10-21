import io.github.lefpap.morningbro.settings.Settings;
import io.github.lefpap.morningbro.settings.SettingsException;
import io.github.lefpap.morningbro.weather.*;

void main() {
    try {
        Settings settings = Settings.load(System.getenv());
        run(settings);
    } catch (SettingsException e) {
        exit(e.getMessage());
    } catch (WeatherException e) {
        exit("The weather is unavailable: " + e.getMessage());
    }
}

void run(Settings settings) {
    OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    WeatherService weatherService = new WeatherService(openMeteoClient);
    WeatherEmoji weatherEmoji = new WeatherEmoji(settings.weather());

    IO.println(settings.summary());

    LocalDate forecastDay = LocalDate.now(settings.schedule().timezone());
    IO.println("Forecast Day: " + forecastDay);
    weatherService.forecasts(settings.cities(), forecastDay)
        .forEach((city, forecast) -> IO.println(describe(city, forecast, weatherEmoji)));
}

/**
 * One city's forecast as the run log shows it, with the Condition and Temperature Alerts the weather rules give it.
 */
String describe(City city, Forecast forecast, WeatherEmoji weatherEmoji) {
    String rainChance = forecast.rainChance().isPresent() ? forecast.rainChance().getAsInt() + "%" : "none";
    return "  %s %s: weather code %d, %s–%s°, Rain Chance %s → %s %s %s".formatted(
        city.flag(), city.name(), forecast.weatherCode(), forecast.low(), forecast.high(), rainChance,
        weatherEmoji.condition(forecast), weatherEmoji.alerts(forecast), weatherEmoji.emoji(forecast));
}

void exit(String message) {
    System.err.println(message);
    System.exit(1);
}
