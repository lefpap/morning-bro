package io.github.lefpap.morningbro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MorningBroPropertiesTest {

    private static final String[] VALID_PROPERTIES = {
        "morning-bro.time-zone=Europe/Athens",
        "morning-bro.home=Athens",
        "morning-bro.message.morning=Good Morning",
        "morning-bro.message.friday=Take it easy Friday!",
        "morning-bro.message.attribution=-# Weather by Open-Meteo",
        "morning-bro.locations[0].name=Athens",
        "morning-bro.locations[0].country-code=GR",
        "morning-bro.locations[0].latitude=37.98",
        "morning-bro.locations[0].longitude=23.73"
    };

    private static final String[] BRUSSELS_PROPERTIES = {
        "morning-bro.locations[1].name=Brussels",
        "morning-bro.locations[1].country-code=BE",
        "morning-bro.locations[1].latitude=50.85",
        "morning-bro.locations[1].longitude=4.35"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsValidConfigurationInLocationOrder() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues(BRUSSELS_PROPERTIES)
            .run(context -> {
                assertThat(context).hasNotFailed();
                MorningBroProperties properties = context.getBean(MorningBroProperties.class);
                assertThat(properties.timeZone()).isEqualTo(ZoneId.of("Europe/Athens"));
                assertThat(properties.home()).isEqualTo("Athens");
                assertThat(properties.message()).isEqualTo(new MorningBroProperties.Message("Good Morning", "Take it easy Friday!", "-# Weather by Open-Meteo"));
                assertThat(properties.locations()).containsExactly(
                    new MorningBroProperties.Location("Athens", "GR", 37.98, 23.73),
                    new MorningBroProperties.Location("Brussels", "BE", 50.85, 4.35));
                assertThat(properties.dryRun()).isFalse();
            });
    }

    @Test
    void bindsDryRun() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.dry-run=true")
            .run(context -> assertThat(context.getBean(MorningBroProperties.class).dryRun()).isTrue());
    }

    @ParameterizedTest
    @CsvSource({
        "morning-bro.time-zone, timeZone",
        "morning-bro.home, home",
        "morning-bro.message.morning, message.morning",
        "morning-bro.message.friday, message.friday",
        "morning-bro.message.attribution, message.attribution",
        "morning-bro.locations[0].name, locations[0].name",
        "morning-bro.locations[0].country-code, locations[0].countryCode",
        "morning-bro.locations[0].latitude, locations[0].latitude",
        "morning-bro.locations[0].longitude, locations[0].longitude"
    })
    void missingRequiredPropertyFailsStartup(String property, String field) {
        String[] properties = Arrays.stream(VALID_PROPERTIES)
            .filter(value -> !value.startsWith(property + "="))
            .toArray(String[]::new);

        runner.withPropertyValues(properties)
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field '" + field + "'"));
    }

    @Test
    void unknownTimeZoneFailsStartup() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.time-zone=Europe/Athen")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("morning-bro.time-zone"));
    }

    @Test
    void missingMessageGroupFailsStartup() {
        String[] properties = Arrays.stream(VALID_PROPERTIES)
            .filter(value -> !value.startsWith("morning-bro.message."))
            .toArray(String[]::new);

        runner.withPropertyValues(properties)
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'message'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"morning", "friday", "attribution"})
    void blankMessageFailsStartup(String key) {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.message." + key + "= ")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'message." + key + "'"));
    }

    // Exact, case-sensitive match against location names.
    @ParameterizedTest
    @ValueSource(strings = {"athens", "ATHENS", "Athen", "Brussels"})
    void unknownHomeFailsStartup(String home) {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.home=" + home)
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("home must match the name of one of the locations"));
    }

    @Test
    void homeMayBeAnyLocation() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues(BRUSSELS_PROPERTIES)
            .withPropertyValues("morning-bro.home=Brussels")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void noLocationsFailsStartup() {
        runner.withPropertyValues("morning-bro.time-zone=Europe/Athens")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'locations'"));
    }

    @Test
    void blankLocationNameFailsStartup() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.locations[0].name= ")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'locations[0].name'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gr", "GRC", "G", "G1", "", " "})
    void countryCodeMustBeTwoUppercaseLetters(String countryCode) {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.locations[0].country-code=" + countryCode)
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'locations[0].countryCode'"));
    }

    @ParameterizedTest
    @CsvSource({"latitude, 90.01", "latitude, -90.01", "longitude, 180.01", "longitude, -180.01"})
    void outOfRangeCoordinateFailsStartup(String coordinate, String value) {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues("morning-bro.locations[0]." + coordinate + "=" + value)
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("field 'locations[0]." + coordinate + "'"));
    }

    @ParameterizedTest
    @CsvSource({"-90, -180", "-90, 180", "90, -180", "90, 180"})
    void boundaryCoordinatesBindWithTheirSigns(double latitude, double longitude) {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues(
                "morning-bro.locations[0].latitude=" + latitude,
                "morning-bro.locations[0].longitude=" + longitude)
            .run(context -> {
                assertThat(context).hasNotFailed();
                MorningBroProperties.Location location = context.getBean(MorningBroProperties.class).locations().getFirst();
                assertThat(location.latitude()).isEqualTo(latitude);
                assertThat(location.longitude()).isEqualTo(longitude);
            });
    }

    @Test
    void duplicateLocationNamesFailStartup() {
        runner.withPropertyValues(VALID_PROPERTIES)
            .withPropertyValues(
                "morning-bro.locations[1].name=Athens",
                "morning-bro.locations[1].country-code=GR",
                "morning-bro.locations[1].latitude=37.98",
                "morning-bro.locations[1].longitude=23.73")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining("location names must be unique"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MorningBroProperties.class)
    static class PropertiesConfig {
    }

}
