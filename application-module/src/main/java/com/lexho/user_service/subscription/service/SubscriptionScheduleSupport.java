package com.lexho.user_service.subscription.service;

import com.lexho.user_service.subscription.exception.SubscriptionValidationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class SubscriptionScheduleSupport {

    private SubscriptionScheduleSupport() {
    }

    static List<String> normalizeDays(List<String> days) {
        if (days == null) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawDay : days) {
            if (rawDay == null || rawDay.isBlank()) {
                continue;
            }
            DayOfWeek dayOfWeek = parseDay(rawDay);
            normalized.add(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        return List.copyOf(normalized);
    }

    static DayOfWeek parseDay(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().equals(normalized)
                    || day.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(Locale.ROOT).equals(normalized)) {
                return day;
            }
        }
        throw new SubscriptionValidationException("Invalid frequency day: " + value);
    }

    static List<LocalDate> generateVisitDates(LocalDate startDate, LocalDate endDate, List<String> frequencyDays) {
        Set<DayOfWeek> weekdays = frequencyDays.stream()
                .map(SubscriptionScheduleSupport::parseDay)
                .collect(Collectors.toSet());
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (weekdays.contains(cursor.getDayOfWeek())) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }
}
