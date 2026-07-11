package com.ericulicny.sun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Location record.
 */
class LocationTest {

    @Test
    @DisplayName("Location stores latitude and longitude correctly")
    void storesCoordinates() {
        Location location = new Location(42.5869, -82.9200);
        assertEquals(42.5869, location.latitude(), 0.0001);
        assertEquals(-82.9200, location.longitude(), 0.0001);
    }

    @Test
    @DisplayName("Two locations with same coordinates are equal")
    void equalityCheck() {
        Location a = new Location(42.5869, -82.9200);
        Location b = new Location(42.5869, -82.9200);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Two locations with different coordinates are not equal")
    void inequalityCheck() {
        Location a = new Location(42.5869, -82.9200);
        Location b = new Location(40.7128, -74.0060);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("toString includes both coordinates")
    void toStringContainsCoordinates() {
        Location location = new Location(42.5869, -82.9200);
        String str = location.toString();
        assertTrue(str.contains("42.5869"), "toString should contain latitude");
        assertTrue(str.contains("-82.92"), "toString should contain longitude");
    }

    @Test
    @DisplayName("Supports extreme coordinates")
    void extremeCoordinates() {
        // North Pole
        Location northPole = new Location(90.0, 0.0);
        assertEquals(90.0, northPole.latitude());

        // International Date Line
        Location dateLine = new Location(0.0, 180.0);
        assertEquals(180.0, dateLine.longitude());

        // Negative longitude (Western hemisphere)
        Location western = new Location(-33.8688, -151.2093);
        assertEquals(-33.8688, western.latitude(), 0.0001);
        assertEquals(-151.2093, western.longitude(), 0.0001);
    }
}
