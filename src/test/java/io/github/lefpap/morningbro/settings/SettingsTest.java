package io.github.lefpap.morningbro.settings;

import io.github.lefpap.morningbro.weather.City;
import io.github.lefpap.morningbro.weather.Condition;
import io.github.lefpap.morningbro.weather.TemperatureAlert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class SettingsTest {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1/token";

    /** The test Settings File, src/test/resources/application-test.yml. Tests change its text to break one rule at a time. */
    private static final String VALID = readTestSettingsFile();

    @TempDir
    Path dir;

    @Test
    void loadsAValidSettingsFileIntoTheFeatureSettings() {
        Settings settings = load(VALID);

        assertThat(settings.schedule().hour()).isEqualTo(8);
        assertThat(settings.schedule().timezone()).isEqualTo(ZoneId.of("Europe/Athens"));
        assertThat(settings.cities()).containsExactly(
                new City("Athens", "🇬🇷", 37.98, 23.73),
                new City("Brussels", "🇧🇪", 50.85, 4.35));

        assertThat(settings.greeting().text()).isEqualTo("Good Morning ☕");
        assertThat(settings.greeting().homeCity()).isEqualTo("Athens");
        assertThat(settings.greeting().friday()).isEqualTo("Take it easy Friday! 😎");
        assertThat(settings.greeting().fallback()).isEqualTo("(weather is taking the day off)");

        assertThat(settings.weather().likelyRainPercent()).isEqualTo(40);
        assertThat(settings.weather().showRainPercent()).isEqualTo(20);
        assertThat(settings.weather().emoji()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                entry(Condition.CLEAR, "☀️"),
                entry(Condition.MOSTLY_CLEAR, "🌤️"),
                entry(Condition.PARTLY_CLOUDY, "⛅"),
                entry(Condition.OVERCAST, "☁️"),
                entry(Condition.FOG, "🌫️"),
                entry(Condition.DRIZZLE, "🌦️"),
                entry(Condition.RAIN, "🌧️"),
                entry(Condition.SNOW, "🌨️"),
                entry(Condition.THUNDERSTORM, "⛈️"),
                entry(Condition.UNKNOWN, "🌡️"),
                entry(Condition.UNLIKELY_RAIN, "🌦️")));
        assertThat(settings.weather().alertEmoji()).containsExactlyInAnyOrderEntriesOf(Map.of(
                TemperatureAlert.HOT, "🥵",
                TemperatureAlert.FREEZING, "🥶"));

        assertThat(settings.discord().username()).isEqualTo("Morning Bro");
        assertThat(settings.discord().avatarUrl()).isEmpty();
        assertThat(settings.discord().color()).isEqualTo("#6F4E37");
        assertThat(settings.discord().footer()).isEqualTo("Forecast by Open-Meteo");
        assertThat(settings.discord().webhookUrl()).contains(WEBHOOK_URL);
    }

    @Test
    void failsWhenTheSettingsFileIsMissing() {
        Path missing = dir.resolve("nowhere.yml");
        Map<String, String> env = env();
        env.put("SETTINGS_PATH", missing.toString());

        assertLoadFails(env).hasMessageContaining("Settings File not found: " + missing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "# only a comment\n", "---\n"})
    void failsWhenTheSettingsFileIsEmpty(String yaml) {
        assertLoadFails(yaml).hasMessageContaining("Settings File is empty: " + dir.resolve("application.yml"));
    }

    @Test
    void failsOnAnUnknownTopLevelKey() {
        assertLoadFails(VALID + "extra: 1\n").hasMessageContaining("unknown key \"extra\"");
    }

    @Test
    void failsOnAnUnknownNestedKey() {
        assertLoadFails(VALID.replace("  friday:", "  colour: red\n  friday:"))
                .hasMessageContaining("unknown key \"greeting.colour\"");
    }

    @Test
    void failsOnAnUnknownEmojiKey() {
        assertLoadFails(VALID.replace("    clear:", "    sunny: \"🌞\"\n    clear:"))
                .hasMessageContaining("unknown key \"weather.emoji.sunny\"");
    }

    @Test
    void failsOnAnUnknownAlertEmojiKey() {
        assertLoadFails(VALID.replace("    hot:", "    windy: \"💨\"\n    hot:"))
                .hasMessageContaining("unknown key \"weather.alert-emoji.windy\"");
    }

    @Test
    void failsOnAMissingKey() {
        assertLoadFails(VALID.replace("  text: \"Good Morning ☕\"\n", ""))
                .hasMessageContaining("missing key \"greeting.text\"");
    }

    @Test
    void failsOnANullValueForARequiredKey() {
        assertLoadFails(VALID.replace("  text: \"Good Morning ☕\"", "  text: null"))
                .hasMessageContaining("missing key \"greeting.text\"");
    }

    @Test
    void failsOnAMissingSection() {
        assertLoadFails(VALID.substring(0, VALID.indexOf("discord:")))
                .hasMessageContaining("missing key \"discord\"");
    }

    @Test
    void readsAnAvatarUrlWhenOneIsSet() {
        Settings settings = load(VALID.replace("avatar-url: null", "avatar-url: \"https://example.com/bro.png\""));

        assertThat(settings.discord().avatarUrl()).contains("https://example.com/bro.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "clear", "mostly-clear", "partly-cloudy", "overcast", "fog", "drizzle",
            "rain", "snow", "thunderstorm", "unknown", "unlikely-rain"})
    void failsWhenAConditionHasNoEmoji(String key) {
        assertLoadFails(withoutLine("    " + key + ": "))
                .hasMessageContaining("weather.emoji is missing an emoji for \"" + key + "\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hot", "freezing"})
    void failsWhenATemperatureAlertHasNoEmoji(String key) {
        assertLoadFails(withoutLine("    " + key + ": "))
                .hasMessageContaining("weather.alert-emoji is missing an emoji for \"" + key + "\"");
    }

    @Test
    void failsWhenTheHomeCityIsNotOneOfTheCities() {
        assertLoadFails(VALID.replace("home-city: Athens", "home-city: Paris"))
                .hasMessageContaining("greeting.home-city \"Paris\" is not one of the cities (Athens, Brussels)");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 24})
    void failsWhenTheHourIsOutside0To23(int hour) {
        assertLoadFails(VALID.replace("hour: 8", "hour: " + hour))
                .hasMessageContaining("schedule.hour must be from 0 to 23, but is " + hour);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 23})
    void acceptsTheFirstAndLastHourOfTheDay(int hour) {
        assertThat(load(VALID.replace("hour: 8", "hour: " + hour)).schedule().hour()).isEqualTo(hour);
    }

    @Test
    void failsOnAnUnknownTimezone() {
        assertLoadFails(VALID.replace("Europe/Athens", "Europe/Atlantis"))
                .hasMessageContaining("schedule.timezone \"Europe/Atlantis\" is not a known timezone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"90.01", "-90.01"})
    void failsWhenALatitudeIsOutOfRange(String lat) {
        assertLoadFails(VALID.replace("lat: 37.98", "lat: " + lat))
                .hasMessageContaining("cities[0] Athens: lat must be from -90 to 90, but is " + lat);
    }

    @ParameterizedTest
    @ValueSource(strings = {"180.01", "-180.01"})
    void failsWhenALongitudeIsOutOfRange(String lon) {
        assertLoadFails(VALID.replace("lon: 4.35", "lon: " + lon))
                .hasMessageContaining("cities[1] Brussels: lon must be from -180 to 180, but is " + lon);
    }

    @Test
    void acceptsCoordinatesAtTheLimits() {
        Settings settings = load(VALID.replace("lat: 37.98", "lat: -90.0").replace("lon: 23.73", "lon: 180.0"));

        assertThat(settings.cities().getFirst()).isEqualTo(new City("Athens", "🇬🇷", -90.0, 180.0));
    }

    @Test
    void failsWhenThereAreNoCities() {
        String yaml = withoutLine("  - {").replace("cities:", "cities: []");

        assertLoadFails(yaml).hasMessageContaining("cities must list at least one city");
    }

    @Test
    void failsOnDuplicateCityNames() {
        assertLoadFails(VALID.replace("name: Brussels", "name: Athens"))
                .hasMessageContaining("cities lists \"Athens\" more than once");
    }

    @Test
    void reportsEveryMistakeAtOnce() {
        String yaml = VALID.replace("hour: 8", "hour: 24").replace("home-city: Athens", "home-city: Paris");

        assertLoadFails(yaml).hasMessage("""
                The settings have mistakes (Settings File: %s):
                  - schedule.hour must be from 0 to 23, but is 24
                  - greeting.home-city "Paris" is not one of the cities (Athens, Brussels)"""
                .formatted(dir.resolve("application.yml")));
    }

    @Test
    void isNeitherForcedNorADryRunByDefault() {
        Settings settings = load(VALID);

        assertThat(settings.force()).isFalse();
        assertThat(settings.dryRun()).isFalse();
    }

    @Test
    void readsForceAndDryRun() {
        Map<String, String> env = env();
        env.put("FORCE", "true");
        env.put("DRY_RUN", "TRUE");

        Settings settings = load(VALID, env);

        assertThat(settings.force()).isTrue();
        assertThat(settings.dryRun()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FORCE", "DRY_RUN"})
    void failsOnAFlagThatIsNotTrueOrFalse(String flag) {
        Map<String, String> env = env();
        env.put(flag, "yes");

        assertLoadFails(VALID, env).hasMessageContaining(flag + " must be \"true\" or \"false\", but is \"yes\"");
    }

    @Test
    void treatsAnEmptyFlagAsUnset() {
        Map<String, String> env = env();
        env.put("FORCE", "");

        assertThat(load(VALID, env).force()).isFalse();
    }

    @Test
    void requiresTheWebhookUrl() {
        Map<String, String> env = env();
        env.remove("DISCORD_WEBHOOK_URL");

        assertLoadFails(VALID, env).hasMessageContaining("DISCORD_WEBHOOK_URL is required unless DRY_RUN is true");
    }

    @Test
    void treatsAnEmptyWebhookUrlAsMissing() {
        Map<String, String> env = env();
        env.put("DISCORD_WEBHOOK_URL", "");

        assertLoadFails(VALID, env).hasMessageContaining("DISCORD_WEBHOOK_URL is required unless DRY_RUN is true");
    }

    @Test
    void doesNotRequireTheWebhookUrlInADryRun() {
        Map<String, String> env = env();
        env.remove("DISCORD_WEBHOOK_URL");
        env.put("DRY_RUN", "true");

        assertThat(load(VALID, env).discord().webhookUrl()).isEmpty();
    }

    @Test
    void readsTheSettingsFileAtSettingsPath() {
        assertThat(load(VALID).source()).isEqualTo(dir.resolve("application.yml").toString());
    }

    @Test
    void loadsTheRealSettingsFileFromTheClasspathByDefault() {
        Settings settings = Settings.load(Map.of("DRY_RUN", "true"));

        assertThat(settings.source()).isEqualTo("classpath:application.yml");
        assertThat(settings.cities()).extracting(City::name).containsExactly("Athens", "Brussels", "Milan");
    }

    @Test
    void summarizesWhatItLoaded() {
        Map<String, String> env = env();
        env.put("DRY_RUN", "true");

        assertThat(load(VALID, env).summary()).isEqualTo("""
                Loaded settings from %s
                  Cities: Athens, Brussels
                  Home City: Athens
                  Post Time: 08:00
                  Reference Timezone: Europe/Athens
                  Forced Post: no
                  Dry Run: yes""".formatted(dir.resolve("application.yml")));
    }

    private static String readTestSettingsFile() {
        try (InputStream in = SettingsTest.class.getResourceAsStream("/application-test.yml")) {
            assertThat(in).isNotNull();
            // Git may check the file out with \r\n line endings; the tests edit its text line by line.
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String withoutLine(String start) {
        return VALID.lines().filter(line -> !line.startsWith(start)).collect(Collectors.joining("\n"));
    }

    private Settings load(String yaml) {
        return load(yaml, env());
    }

    private Settings load(String yaml, Map<String, String> env) {
        write(yaml);
        return Settings.load(env);
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertLoadFails(String yaml) {
        return assertLoadFails(yaml, env());
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertLoadFails(String yaml, Map<String, String> env) {
        write(yaml);
        return assertLoadFails(env);
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertLoadFails(Map<String, String> env) {
        return assertThatThrownBy(() -> Settings.load(env)).isInstanceOf(SettingsException.class);
    }

    private Map<String, String> env() {
        Map<String, String> env = new HashMap<>();
        env.put("SETTINGS_PATH", dir.resolve("application.yml").toString());
        env.put("DISCORD_WEBHOOK_URL", WEBHOOK_URL);
        return env;
    }

    private void write(String yaml) {
        try {
            Files.writeString(dir.resolve("application.yml"), yaml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
