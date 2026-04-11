package com.example.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class IconPackResourceTest {

    @Test
    void everyIconPackContainsEveryStandardIconResource() {
        for (IconPack pack : IconPack.supportedValues()) {
            for (IconKey iconKey : IconKey.supportedValues()) {
                String resourcePath = pack.resolveResourcePath(iconKey);
                assertNotNull(
                    getClass().getResource(resourcePath),
                    () -> "Missing icon resource: " + resourcePath
                );
            }
        }
    }

    @Test
    void everyDarkIconPackContainsEveryStandardIconResourceWhenPresent() {
        for (IconPack pack : IconPack.supportedValues()) {
            String marker = "/icons/" + pack.getId() + "_dark/" + IconKey.CALENDAR.getFileName();
            if (getClass().getResource(marker) == null) {
                continue;
            }
            for (IconKey iconKey : IconKey.supportedValues()) {
                String resourcePath = "/icons/" + pack.getId() + "_dark/" + iconKey.getFileName();
                assertNotNull(
                    getClass().getResource(resourcePath),
                    () -> "Missing dark icon resource: " + resourcePath
                );
            }
        }
    }
}
