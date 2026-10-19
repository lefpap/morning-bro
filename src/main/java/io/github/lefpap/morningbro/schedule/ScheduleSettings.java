package io.github.lefpap.morningbro.schedule;

import java.time.ZoneId;

public record ScheduleSettings(int hour, ZoneId timezone) {
}
