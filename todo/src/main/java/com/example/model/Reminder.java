package com.example.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Reminder {
    public static final String DEFAULT_CHANNEL = "in_app";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_DISMISSED = "dismissed";

    private String id;
    private String scheduleItemId;
    private LocalDateTime remindAtUtc;
    private Integer offsetMinutes;
    private String channel;
    private String status;
    private LocalDateTime createdAtUtc;
    private LocalDateTime updatedAtUtc;

    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.channel = DEFAULT_CHANNEL;
        this.status = STATUS_ACTIVE;
        LocalDateTime now = Tag.nowUtc();
        this.createdAtUtc = now;
        this.updatedAtUtc = now;
    }

    public Reminder(LocalDateTime remindAtUtc) {
        this();
        setRemindAtUtc(remindAtUtc);
    }

    public Reminder(Reminder source) {
        this.id = source != null ? source.id : UUID.randomUUID().toString();
        this.scheduleItemId = source != null ? source.scheduleItemId : null;
        this.remindAtUtc = source != null ? source.remindAtUtc : null;
        this.offsetMinutes = source != null ? source.offsetMinutes : null;
        this.channel = source != null ? source.channel : DEFAULT_CHANNEL;
        this.status = source != null ? source.status : STATUS_ACTIVE;
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

    public LocalDateTime getRemindAtUtc() {
        return remindAtUtc;
    }

    public void setRemindAtUtc(LocalDateTime remindAtUtc) {
        this.remindAtUtc = Tag.truncateSeconds(remindAtUtc);
    }

    public Integer getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(Integer offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        String normalized = Tag.normalizeNullableText(channel);
        this.channel = normalized != null ? normalized : DEFAULT_CHANNEL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String normalized = Tag.normalizeNullableText(status);
        this.status = normalized != null ? normalized : STATUS_ACTIVE;
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

    public Reminder copy() {
        return new Reminder(this);
    }
}
