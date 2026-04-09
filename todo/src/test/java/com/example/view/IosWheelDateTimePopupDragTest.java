package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.IntFunction;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class IosWheelDateTimePopupDragTest {

    @BeforeAll
    static void initializeToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit is already running in this JVM.
        }
    }

    @Test
    void indexForOffsetRoundsToNearestEntry() throws Exception {
        Class<?> columnClass = Class.forName("com.example.view.IosWheelDateTimePopup$WheelColumn");
        Constructor<?> constructor = columnClass.getDeclaredConstructor(double.class, IntFunction.class);
        constructor.setAccessible(true);
        Object column = constructor.newInstance(40.0, (IntFunction<String>) String::valueOf);

        Method setItems = columnClass.getDeclaredMethod("setItems", List.class, int.class);
        setItems.setAccessible(true);
        setItems.invoke(column, List.of(1900, 1901, 1902, 1903, 1904), 1902);

        Method indexForOffset = columnClass.getDeclaredMethod("indexForOffset", double.class);
        indexForOffset.setAccessible(true);

        assertEquals(0, indexForOffset.invoke(column, 0.1));
        assertEquals(2, indexForOffset.invoke(column, 2 * 40 + 15));
        assertEquals(4, indexForOffset.invoke(column, 4 * 40 + 20));
    }

    @Test
    void indexForOffsetClampsBeyondBounds() throws Exception {
        Class<?> columnClass = Class.forName("com.example.view.IosWheelDateTimePopup$WheelColumn");
        Constructor<?> constructor = columnClass.getDeclaredConstructor(double.class, IntFunction.class);
        constructor.setAccessible(true);
        Object column = constructor.newInstance(40.0, (IntFunction<String>) String::valueOf);

        Method setItems = columnClass.getDeclaredMethod("setItems", List.class, int.class);
        setItems.setAccessible(true);
        setItems.invoke(column, List.of(1900, 1901, 1902, 1903, 1904), 1902);

        Method indexForOffset = columnClass.getDeclaredMethod("indexForOffset", double.class);
        indexForOffset.setAccessible(true);

        assertEquals(0, indexForOffset.invoke(column, -60.0));
        assertEquals(4, indexForOffset.invoke(column, 10 * 40.0));
    }
}
