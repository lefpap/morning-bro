package io.github.lefpap.morningbro;

import io.github.lefpap.morningbro.MorningBroProperties.Location;
import io.github.lefpap.morningbro.message.LocationWeather;
import io.github.lefpap.morningbro.message.MorningMessageComposer;
import io.github.lefpap.morningbro.notification.MessagePublisher;
import io.github.lefpap.morningbro.weather.Forecast;
import io.github.lefpap.morningbro.weather.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class MorningBroJobTest {

    private static final Location ATHENS = new Location("Athens", "GR", 37.98, 23.73);
    private static final Location BRUSSELS = new Location("Brussels", "BE", 50.85, 4.35);
    private static final Location MILAN = new Location("Milan", "IT", 45.46, 9.19);

    private static final Forecast ATHENS_FORECAST = new Forecast("⛅", 15, 21, 25);
    private static final Forecast BRUSSELS_FORECAST = new Forecast("🌦️", 6, 11, 60);
    private static final Forecast MILAN_FORECAST = new Forecast("🌧️", 9, 14, 80);

    private static final MorningBroProperties REAL_RUN = properties(false);
    private static final MorningBroProperties DRY_RUN = properties(true);

    private static final String FAILED_LINE = "(weather is taking the day off)";

    // Fixed clocks are in UTC on purpose, so the tests prove the job uses the configured zone instead.
    private static final Instant THURSDAY_MORNING = Instant.parse("2026-10-29T06:00:00Z");

    private final WeatherService weather = mock(WeatherService.class);
    private final MessagePublisher publisher = mock(MessagePublisher.class);
    private final MorningMessageComposer composer = new MorningMessageComposer(REAL_RUN);

    @BeforeEach
    void allLocationsHaveWeather() {
        when(weather.today(ATHENS)).thenReturn(Optional.of(ATHENS_FORECAST));
        when(weather.today(BRUSSELS)).thenReturn(Optional.of(BRUSSELS_FORECAST));
        when(weather.today(MILAN)).thenReturn(Optional.of(MILAN_FORECAST));
    }

    @Test
    void publishesComposedMessageOnce() {
        run(THURSDAY_MORNING);

        String expected = composer.compose(LocalDate.of(2026, 10, 29), List.of(
            new LocationWeather(ATHENS, Optional.of(ATHENS_FORECAST)),
            new LocationWeather(BRUSSELS, Optional.of(BRUSSELS_FORECAST)),
            new LocationWeather(MILAN, Optional.of(MILAN_FORECAST))));
        assertThat(published()).isEqualTo(expected);
    }

    @Test
    void dryRunLogsInsteadOfPublishing(CapturedOutput output) {
        // A dry run has no publisher bean: an empty registry fails the test if the job ever asks for one.
        ObjectProvider<MessagePublisher> missingPublisher = new StaticListableBeanFactory().getBeanProvider(MessagePublisher.class);
        run(DRY_RUN, missingPublisher, THURSDAY_MORNING);

        assertThat(output).contains("Dry run, not posting morning message:\nGood Morning⛅\n\n🇬🇷 Athens");
        assertThat(output).contains("weather for 3 of 3 locations");
    }

    @Test
    void oneLocationFailureStillPublishes() {
        when(weather.today(BRUSSELS)).thenThrow(new ResourceAccessException("I/O error on GET request"));

        run(THURSDAY_MORNING);

        assertThat(published())
            .contains("🇧🇪 Brussels\n" + FAILED_LINE)
            .contains("🇬🇷 Athens\n⛅ · 15°–21° · 💧 25%")
            .contains("🇮🇹 Milan\n🌧️ · 9°–14° · 💧 80%");
    }

    @Test
    void nonHttpFailureIsIsolatedToo() {
        when(weather.today(MILAN)).thenThrow(new IllegalStateException("bad data"));

        run(THURSDAY_MORNING);

        assertThat(published()).contains("🇮🇹 Milan\n" + FAILED_LINE);
    }

    @Test
    void allLocationsFailedStillGreets() {
        when(weather.today(ATHENS)).thenThrow(new ResourceAccessException("I/O error on GET request"));
        when(weather.today(BRUSSELS)).thenThrow(new ResourceAccessException("I/O error on GET request"));
        when(weather.today(MILAN)).thenThrow(new ResourceAccessException("I/O error on GET request"));

        run(THURSDAY_MORNING);

        String message = published();
        assertThat(message).startsWith("Good Morning\n\n");
        assertThat(message.split("\\Q" + FAILED_LINE + "\\E", -1)).hasSize(4);
    }

    @Test
    void fridayInAthensWhileThursdayInUtc() {
        run(Instant.parse("2026-10-29T22:30:00Z"));

        assertThat(firstLine()).isEqualTo("Good Morning⛅ Take it easy Friday!");
        verify(weather).today(ATHENS);
        verify(weather).today(BRUSSELS);
        verify(weather).today(MILAN);
    }

    @Test
    void thursdayLateEveningInAthensIsNotFriday() {
        run(Instant.parse("2026-10-29T21:30:00Z"));

        assertThat(firstLine()).isEqualTo("Good Morning⛅");
    }

    @Test
    void publishFailurePropagates() {
        RuntimeException failure =
            new IllegalStateException("Webhook failed (500 INTERNAL_SERVER_ERROR): Internal Server Error");
        doThrow(failure).when(publisher).publish(anyString());

        assertThatThrownBy(() -> run(THURSDAY_MORNING)).isSameAs(failure);
    }

    @Test
    void logsWeatherFailureWithLocationAndReason(CapturedOutput output) {
        when(weather.today(BRUSSELS)).thenThrow(new ResourceAccessException("I/O error on GET request"));

        run(THURSDAY_MORNING);

        assertThat(output).contains(
            "Weather unavailable for Brussels: org.springframework.web.client.ResourceAccessException: I/O error on GET request");
    }

    @Test
    void logsHowManyLocationsHadWeather(CapturedOutput output) {
        when(weather.today(MILAN)).thenThrow(new ResourceAccessException("I/O error on GET request"));

        run(THURSDAY_MORNING);

        assertThat(output).contains("Morning message published with weather for 2 of 3 locations");
    }

    @Test
    void noCountLoggedWhenPublishFails(CapturedOutput output) {
        doThrow(new IllegalStateException("Webhook failed (500 INTERNAL_SERVER_ERROR): Internal Server Error"))
            .when(publisher).publish(anyString());

        assertThatThrownBy(() -> run(THURSDAY_MORNING)).isInstanceOf(IllegalStateException.class);
        assertThat(output).doesNotContain("Morning message published");
    }

    private void run(Instant now) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory(Map.of("publisher", publisher));
        run(REAL_RUN, beans.getBeanProvider(MessagePublisher.class), now);
    }

    private void run(MorningBroProperties properties, ObjectProvider<MessagePublisher> publisher, Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        new MorningBroJob(properties, weather, composer, publisher, clock).run(new DefaultApplicationArguments());
    }

    private String published() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(message.capture());
        return message.getValue();
    }

    private String firstLine() {
        return published().lines().findFirst().orElseThrow();
    }

    private static MorningBroProperties properties(boolean dryRun) {
        MorningBroProperties.Message message = new MorningBroProperties.Message(
            "Good Morning",
            "Take it easy Friday!",
            "-# Weather by Open-Meteo"
        );

        return new MorningBroProperties(
            ZoneId.of("Europe/Athens"),
            "Athens",
            message,
            List.of(ATHENS, BRUSSELS, MILAN),
            dryRun);
    }

}
