package com.lexho.user_service.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerShiftStartRequest {
    public LocalDateTime shiftStart;
    public LocalDateTime shiftEnd;

    // Compatibility payload variants from frontend
    public String shiftDate;
    public String shiftStartDate;
    public String date;
    public String startTime;
    public String endTime;
    public String start;
    public String end;
    public String shiftStartTime;
    public String shiftEndTime;

    @JsonIgnore
    public LocalDateTime resolveShiftStart() {
        if (shiftStart != null) {
            return shiftStart;
        }
        String resolvedDate = firstNonBlank(shiftDate, shiftStartDate, date);
        String resolvedStart = firstNonBlank(startTime, start, shiftStartTime);
        if (resolvedDate == null || resolvedStart == null) {
            return null;
        }
        return combineDateTime(resolvedDate, resolvedStart);
    }

    @JsonIgnore
    public LocalDateTime resolveShiftEnd() {
        if (shiftEnd != null) {
            return shiftEnd;
        }
        String resolvedDate = firstNonBlank(shiftDate, shiftStartDate, date);
        String resolvedEnd = firstNonBlank(endTime, end, shiftEndTime);
        if (resolvedDate == null || resolvedEnd == null) {
            return null;
        }
        return combineDateTime(resolvedDate, resolvedEnd);
    }

    private LocalDateTime combineDateTime(String dateValue, String timeValue) {
        try {
            LocalDate datePart = LocalDate.parse(dateValue);
            LocalTime timePart = LocalTime.parse(timeValue, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(datePart, timePart);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
