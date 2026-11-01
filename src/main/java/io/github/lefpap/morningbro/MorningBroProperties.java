package io.github.lefpap.morningbro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.util.List;

@Validated
@ConfigurationProperties("morning-bro")
public record MorningBroProperties(
        @NotNull ZoneId timeZone,
        @NotBlank String home,
        @NotNull @Valid Message message,
        @NotEmpty List<@Valid Location> locations,
        boolean dryRun) {

    public record Message(
            @NotBlank String morning,
            @NotBlank String friday) {
    }

    public record Location(
            @NotBlank String name,
            @NotNull @Pattern(regexp = "[A-Z]{2}") String countryCode,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {
    }

    // Location names identify locations (home-location refers to one), so they must be unique.
    @AssertTrue(message = "location names must be unique")
    public boolean isLocationNamesUnique() {
        if (locations == null) {
            return true;
        }
        return locations.stream().map(Location::name).distinct().count() == locations.size();
    }

    // home must name a location exactly (case-sensitive), so a typo fails startup instead of dropping the header emoji.
    @AssertTrue(message = "home must match the name of one of the locations")
    public boolean isHomeKnown() {
        if (home == null || locations == null) {
            return true;
        }
        return locations.stream().map(Location::name).anyMatch(home::equals);
    }

}
