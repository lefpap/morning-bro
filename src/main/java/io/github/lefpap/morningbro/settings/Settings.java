package io.github.lefpap.morningbro.settings;

import io.github.lefpap.morningbro.messaging.discord.DiscordSettings;
import io.github.lefpap.morningbro.messaging.GreetingSettings;
import io.github.lefpap.morningbro.schedule.ScheduleSettings;
import io.github.lefpap.morningbro.weather.City;
import io.github.lefpap.morningbro.weather.Condition;
import io.github.lefpap.morningbro.weather.TemperatureAlert;
import io.github.lefpap.morningbro.weather.WeatherSettings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads the Settings File and the environment variables, and hands each feature its own settings.
 */
public record Settings(
    String source,
    ScheduleSettings schedule,
    List<City> cities,
    GreetingSettings greeting,
    WeatherSettings weather,
    DiscordSettings discord,
    boolean force,
    boolean dryRun) {

    /**
     * Read from the classpath (inside the jar) unless SETTINGS_PATH points to a file on disk.
     */
    private static final String BUNDLED_SETTINGS_FILE = "application.yml";

    private static final YAMLMapper MAPPER = YAMLMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
        .build();

    public static Settings load(Map<String, String> env) {
        String settingsPath = env.getOrDefault("SETTINGS_PATH", "");
        String source = settingsPath.isBlank() ? "classpath:" + BUNDLED_SETTINGS_FILE : settingsPath;
        SettingsFile file = parse(source, read(source, settingsPath));

        List<String> errors = new ArrayList<>();
        boolean force = flag(env, "FORCE", errors);
        boolean dryRun = flag(env, "DRY_RUN", errors);
        Optional<String> webhookUrl = webhookUrl(env, dryRun, errors);
        ZoneId timezone = checkSchedule(file.schedule(), errors);
        checkCities(file.cities(), file.greeting().homeCity(), errors);
        Map<Condition, String> emoji =
            emojiByKey(Condition.class, file.weather().emoji(), "weather.emoji", errors);
        Map<TemperatureAlert, String> alertEmoji =
            emojiByKey(TemperatureAlert.class, file.weather().alertEmoji(), "weather.alert-emoji", errors);

        if (!errors.isEmpty()) {
            throw new SettingsException(errors.stream()
                .map(error -> "  - " + error)
                .collect(Collectors.joining("\n", "The settings have mistakes (Settings File: " + source + "):\n", "")));
        }

        return new Settings(
            source,
            new ScheduleSettings(file.schedule().hour(), timezone),
            List.copyOf(file.cities()),
            new GreetingSettings(
                file.greeting().text(),
                file.greeting().homeCity(),
                file.greeting().friday(),
                file.greeting().fallback()),
            new WeatherSettings(
                file.weather().likelyRainPercent(),
                file.weather().showRainPercent(),
                emoji,
                alertEmoji,
                file.weather().attribution(),
                file.weather().attributionUrl()),
            new DiscordSettings(
                file.discord().username(),
                file.discord().avatarUrl(),
                webhookUrl),
            force,
            dryRun);
    }

    /**
     * What was loaded, for the Owner to read in the run log.
     */
    public String summary() {
        return """
            Loaded settings from %s
              Cities: %s
              Home City: %s
              Post Time: %02d:00
              Reference Timezone: %s
              Forced Post: %s
              Dry Run: %s""".formatted(
            source,
            cities.stream().map(City::name).collect(Collectors.joining(", ")),
            greeting.homeCity(),
            schedule.hour(),
            schedule.timezone(),
            force ? "yes" : "no",
            dryRun ? "yes" : "no");
    }

    /**
     * A true/false environment variable. Unset or empty means false.
     */
    private static boolean flag(Map<String, String> env, String name, List<String> errors) {
        String value = env.getOrDefault(name, "");
        if (value.isBlank() || value.equalsIgnoreCase("false")) {
            return false;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        errors.add("%s must be \"true\" or \"false\", but is \"%s\"".formatted(name, value));
        return false;
    }

    private static Optional<String> webhookUrl(Map<String, String> env, boolean dryRun, List<String> errors) {
        Optional<String> url = Optional.ofNullable(env.get("DISCORD_WEBHOOK_URL")).filter(value -> !value.isBlank());
        if (url.isEmpty() && !dryRun) {
            errors.add("DISCORD_WEBHOOK_URL is required unless DRY_RUN is true");
        }
        return url;
    }

    /**
     * Checks the Post Time and returns the Reference Timezone, or null if it isn't a known timezone.
     */
    private static ZoneId checkSchedule(SettingsFile.Schedule schedule, List<String> errors) {
        if (schedule.hour() < 0 || schedule.hour() > 23) {
            errors.add("schedule.hour must be from 0 to 23, but is " + schedule.hour());
        }
        try {
            return ZoneId.of(schedule.timezone());
        } catch (DateTimeException _) {
            errors.add("schedule.timezone \"%s\" is not a known timezone".formatted(schedule.timezone()));
            return null;
        }
    }

    private static void checkCities(List<City> cities, String homeCity, List<String> errors) {
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            checkCoordinate(i, city, "lat", city.lat(), 90, errors);
            checkCoordinate(i, city, "lon", city.lon(), 180, errors);
        }

        List<String> names = cities.stream().map(City::name).toList();
        if (names.isEmpty()) {
            errors.add("cities must list at least one city");
            return;
        }
        Set<String> seen = new HashSet<>();
        names.stream()
            .filter(name -> !seen.add(name))
            .distinct()
            .forEach(name -> errors.add("cities lists \"%s\" more than once".formatted(name)));
        if (!names.contains(homeCity)) {
            errors.add("greeting.home-city \"%s\" is not one of the cities (%s)"
                .formatted(homeCity, String.join(", ", names)));
        }
    }

    private static void checkCoordinate(int index, City city, String key, double value, int limit, List<String> errors) {
        if (value < -limit || value > limit) {
            errors.add("cities[%d] %s: %s must be from -%d to %d, but is %s"
                .formatted(index, city.name(), key, limit, limit, value));
        }
    }

    private static <E extends Enum<E>> Map<E, String> emojiByKey(
        Class<E> type, Map<String, String> emoji, String path, List<String> errors) {
        Map<String, E> constantsByKey = Arrays.stream(type.getEnumConstants())
            .collect(Collectors.toMap(Settings::keyOf, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        emoji.keySet().stream()
            .filter(key -> !constantsByKey.containsKey(key))
            .forEach(key -> errors.add("unknown key \"%s.%s\"".formatted(path, key)));

        Map<E, String> byConstant = new EnumMap<>(type);
        constantsByKey.forEach((key, constant) -> {
            String value = emoji.get(key);
            if (value == null || value.isBlank()) {
                errors.add("%s is missing an emoji for \"%s\"".formatted(path, key));
            }
            byConstant.put(constant, value);
        });
        return byConstant;
    }

    /**
     * The Settings File key for an enum constant: {@code MOSTLY_CLEAR} is {@code mostly-clear}.
     */
    private static String keyOf(Enum<?> constant) {
        return constant.name().toLowerCase().replace('_', '-');
    }

    private static String read(String source, String settingsPath) {
        try (InputStream in = settingsPath.isBlank()
            ? Settings.class.getClassLoader().getResourceAsStream(BUNDLED_SETTINGS_FILE)
            : Files.newInputStream(Path.of(settingsPath))) {
            if (in == null) {
                throw notFound(source);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (NoSuchFileException _) {
            throw notFound(source);
        } catch (IOException e) {
            throw new SettingsException("Cannot read the Settings File " + source + ": " + e.getMessage());
        }
    }

    private static SettingsException notFound(String source) {
        return new SettingsException("Settings File not found: " + source);
    }

    private static SettingsFile parse(String source, String content) {
        try {
            JsonNode root = MAPPER.readTree(content);
            if (root == null || root.isMissingNode() || root.isNull()) {
                throw new SettingsException("Settings File is empty: " + source);
            }
            return MAPPER.treeToValue(root, SettingsFile.class);
        } catch (UnrecognizedPropertyException e) {
            throw new SettingsException("%s: unknown key \"%s\"".formatted(source, pathOf(e)));
        } catch (MismatchedInputException e) {
            if (isMissing(e)) {
                throw new SettingsException("%s: missing key \"%s\"".formatted(source, pathOf(e)));
            }
            throw invalidValue(source, e);
        } catch (JacksonException e) {
            throw invalidValue(source, e);
        }
    }

    private static SettingsException invalidValue(String source, JacksonException e) {
        return new SettingsException("%s: invalid value at \"%s\": %s"
            .formatted(source, pathOf(e), e.getOriginalMessage()));
    }

    /**
     * Jackson reports a missing or null required key only through the message of a
     * {@link MismatchedInputException}, from FAIL_ON_MISSING_CREATOR_PROPERTIES and FAIL_ON_NULL_CREATOR_PROPERTIES.
     */
    private static boolean isMissing(MismatchedInputException e) {
        String message = e.getOriginalMessage();
        return message.startsWith("Missing creator property") || message.startsWith("Null value for creator property");
    }

    /**
     * Where in the file an error is, as a key path like {@code weather.emoji.sunny} or {@code cities[1].lat}.
     */
    private static String pathOf(JacksonException e) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : e.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }
}
