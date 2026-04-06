package com.example.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class Tag {
    private String id;
    private String name;
    private String color;
    private LocalDateTime createdAtUtc;
    private LocalDateTime updatedAtUtc;

    public Tag() {
        this.id = UUID.randomUUID().toString();
        LocalDateTime now = nowUtc();
        this.createdAtUtc = now;
        this.updatedAtUtc = now;
    }

    public Tag(String name) {
        this();
        setName(name);
    }

    public Tag(Tag source) {
        this.id = source != null ? source.id : UUID.randomUUID().toString();
        this.name = source != null ? source.name : "";
        this.color = source != null ? source.color : null;
        this.createdAtUtc = source != null ? source.createdAtUtc : nowUtc();
        this.updatedAtUtc = source != null ? source.updatedAtUtc : nowUtc();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = normalizeId(id);
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = normalizeText(name);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = normalizeNullableText(color);
    }

    public LocalDateTime getCreatedAtUtc() {
        return createdAtUtc;
    }

    public void setCreatedAtUtc(LocalDateTime createdAtUtc) {
        this.createdAtUtc = truncateSeconds(createdAtUtc);
    }

    public LocalDateTime getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(LocalDateTime updatedAtUtc) {
        this.updatedAtUtc = truncateSeconds(updatedAtUtc);
    }

    public Tag copy() {
        return new Tag(this);
    }

    static LocalDateTime truncateSeconds(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS);
    }

    static String normalizeText(String value) {
        return value == null ? "" : value.strip();
    }

    static String normalizeNullableText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    static String normalizeId(String id) {
        String normalized = normalizeNullableText(id);
        return normalized != null ? normalized : UUID.randomUUID().toString();
    }

    static LocalDateTime nowUtc() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
