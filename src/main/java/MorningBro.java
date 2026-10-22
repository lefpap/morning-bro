import io.github.lefpap.morningbro.messaging.MessagePublisher;
import io.github.lefpap.morningbro.messaging.MorningMessage;
import io.github.lefpap.morningbro.messaging.MorningMessageFactory;
import io.github.lefpap.morningbro.messaging.console.ConsoleMessageComposer;
import io.github.lefpap.morningbro.messaging.console.ConsoleMessagePublisher;
import io.github.lefpap.morningbro.messaging.discord.DiscordMessageComposer;
import io.github.lefpap.morningbro.messaging.discord.DiscordException;
import io.github.lefpap.morningbro.messaging.discord.DiscordMessagePublisher;
import io.github.lefpap.morningbro.schedule.SendWindow;
import io.github.lefpap.morningbro.settings.Settings;
import io.github.lefpap.morningbro.settings.SettingsException;
import io.github.lefpap.morningbro.weather.*;

void main() {
    try {
        Settings settings = Settings.load(System.getenv());
        run(settings);
    } catch (SettingsException e) {
        exit(e.getMessage());
    } catch (DiscordException e) {
        exit("The Morning Message wasn't posted: " + e.getMessage());
    }
}

void run(Settings settings) {
    IO.println(settings.summary());

    Instant now = Instant.now();
    SendWindow sendWindow = new SendWindow(settings.schedule(), settings.force());

    if (!sendWindow.isOpen(now)) {
        IO.println("It isn't the Post Time, so nothing is posted.");
        return;
    }

    OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    WeatherService weatherService = new WeatherService(openMeteoClient);
    MorningMessageFactory messageFactory = new MorningMessageFactory(settings.greeting(), settings.weather());
    MessagePublisher consolePublisher = new ConsoleMessagePublisher(new ConsoleMessageComposer());
    MessagePublisher discordPublisher =
        new DiscordMessagePublisher(new DiscordMessageComposer(settings.discord()), settings.discord(), settings.dryRun());

    LocalDate forecastDay = LocalDate.ofInstant(now, settings.schedule().timezone());
    IO.println("Forecast Day: " + forecastDay);
    Map<City, Forecast> forecastsByCity = fetchForecastsByCity(weatherService, settings.cities(), forecastDay);
    MorningMessage message = messageFactory.create(forecastsByCity, forecastDay);
    IO.println();
    consolePublisher.publish(message);
    IO.println();
    discordPublisher.publish(message);
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
