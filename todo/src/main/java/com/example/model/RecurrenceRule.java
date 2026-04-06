package com.example.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RecurrenceRule {
    public static final String FREQ_DAILY = "daily";
    public static final String FREQ_WEEKLY = "weekly";
    public static final String FREQ_MONTHLY = "monthly";
    public static final String FREQ_YEARLY = "yearly";

    private String id;
    private String scheduleItemId;
    private String frequency = FREQ_DAILY;
    private int interval = 1;
    private final LinkedHashSet<DayOfWeek> byDays = new LinkedHashSet<>();
    private Integer byMonthDay;
    private LocalDateTime untilAtUtc;
    private Integer occurrenceCount;
    private String timezone;
    private boolean active = true;
    private LocalDateTime createdAtUtc;
    private LocalDateTime updatedAtUtc;

    public RecurrenceRule() {
        this.id = UUID.randomUUID().toString();
        LocalDateTime now = Tag.nowUtc();
        this.createdAtUtc = now;
        this.updatedAtUtc = now;
    }

    public RecurrenceRule(RecurrenceRule source) {
        this.id = source != null ? source.id : UUID.randomUUID().toString();
        this.scheduleItemId = source != null ? source.scheduleItemId : null;
        this.frequency = source != null ? source.frequency : FREQ_DAILY;
        this.interval = source != null ? source.interval : 1;
        if (source != null) {
            this.byDays.addAll(source.byDays);
        }
        this.byMonthDay = source != null ? source.byMonthDay : null;
        this.untilAtUtc = source != null ? source.untilAtUtc : null;
        this.occurrenceCount = source != null ? source.occurrenceCount : null;
        this.timezone = source != null ? source.timezone : null;
        this.active = source == null || source.active;
        this.createdAtUtc = source != null ? source.createdAtUtc : Tag.nowUtc();
        this.updatedAtUtc = source != null ? source.updatedAtUtc : Tag.nowUtc();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Tag.normalizeId(id);
    }

    public String getScheduleItemId() {
        return scheduleItemId;
    }

    public void setScheduleItemId(String scheduleItemId) {
        this.scheduleItemId = Tag.normalizeNullableText(scheduleItemId);
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        String normalized = Tag.normalizeNullableText(frequency);
        if (normalized == null) {
            this.frequency = FREQ_DAILY;
            return;
        }
        this.frequency = normalized.toLowerCase(Locale.ROOT);
    }

    public int getInterval() {
        return Math.max(1, interval);
    }

    public void setInterval(int interval) {
        this.interval = Math.max(1, interval);
    }

    public Set<DayOfWeek> getByDays() {
        return Set.copyOf(byDays);
    }

    public void setByDays(Collection<DayOfWeek> byDays) {
        this.byDays.clear();
        if (byDays == null) {
            return;
        }
        this.byDays.addAll(
            byDays.stream()
                .filter(day -> day != null)
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .collect(Collectors.toList())
        );
    }

    public String getByDayCsv() {
        return byDays.stream()
            .sorted(Comparator.comparingInt(DayOfWeek::getValue))
            .map(DayOfWeek::name)
            .collect(Collectors.joining(","));
    }

    public void setByDayCsv(String byDayCsv) {
        if (byDayCsv == null || byDayCsv.isBlank()) {
            byDays.clear();
            return;
        }
        LinkedHashSet<DayOfWeek> parsedDays = new LinkedHashSet<>();
        for (String token : byDayCsv.split(",")) {
            String normalized = Tag.normalizeNullableText(token);
            if (normalized == null) {
                continue;
            }
            parsedDays.add(DayOfWeek.valueOf(normalized.toUpperCase(Locale.ROOT)));
        }
        setByDays(parsedDays);
    }

    public Integer getByMonthDay() {
        return byMonthDay;
    }

    public void setByMonthDay(Integer byMonthDay) {
        this.byMonthDay = byMonthDay;
    }

    public LocalDateTime getUntilAtUtc() {
        return untilAtUtc;
    }

    public void setUntilAtUtc(LocalDateTime untilAtUtc) {
        this.untilAtUtc = Tag.truncateSeconds(untilAtUtc);
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = Tag.normalizeNullableText(timezone);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAtUtc() {
        return createdAtUtc;
    }

    public void setCreatedAtUtc(LocalDateTime createdAtUtc) {
        this.createdAtUtc = Tag.truncateSeconds(createdAtUtc);
    }

    public LocalDateTime getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(LocalDateTime updatedAtUtc) {
        this.updatedAtUtc = Tag.truncateSeconds(updatedAtUtc);
    }

    public boolean hasByDays() {
        return !byDays.isEmpty();
    }

    public boolean hasEndingConstraint() {
        return untilAtUtc != null || (occurrenceCount != null && occurrenceCount > 0);
    }

    public String describe() {
        if (!active) {
            return "未启用重复";
        }
        StringBuilder builder = new StringBuilder();
        switch (getFrequency()) {
            case FREQ_WEEKLY -> {
                builder.append(getInterval() == 1 ? "每周" : "每 ").append(getInterval()).append(" 周");
                if (!byDays.isEmpty()) {
                    String byDayText = byDays.stream()
                        .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                        .map(day -> day.getDisplayName(TextStyle.SHORT, Locale.CHINA))
                        .collect(Collectors.joining("、"));
                    builder.append(" ").append(byDayText);
                }
            }
            case FREQ_MONTHLY -> {
                builder.append(getInterval() == 1 ? "每月" : "每 ").append(getInterval()).append(" 月");
                if (byMonthDay != null) {
                    builder.append(" 第 ").append(byMonthDay).append(" 天");
                }
            }
            case FREQ_YEARLY -> builder.append(getInterval() == 1 ? "每年" : "每 ").append(getInterval()).append(" 年");
            default -> builder.append(getInterval() == 1 ? "每天" : "每 ").append(getInterval()).append(" 天");
        }

        if (untilAtUtc != null) {
            builder.append("，直到 ").append(untilAtUtc.toLocalDate());
        } else if (occurrenceCount != null && occurrenceCount > 0) {
            builder.append("，共 ").append(occurrenceCount).append(" 次");
        }
        return builder.toString();
    }

    public RecurrenceRule copy() {
        return new RecurrenceRule(this);
    }

    public static List<String> supportedFrequencies() {
        return List.of(FREQ_DAILY, FREQ_WEEKLY, FREQ_MONTHLY, FREQ_YEARLY);
    }
}
