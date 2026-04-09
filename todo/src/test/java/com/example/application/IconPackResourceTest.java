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
}
