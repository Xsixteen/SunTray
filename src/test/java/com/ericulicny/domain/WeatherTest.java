package com.ericulicny.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Weather record.
 */
class WeatherTest {

    @Test
    @DisplayName("Weather record stores all fields correctly")
    void storesAllFields() {
        Weather weather = new Weather(72, 80, 55, "Clear", 0.0, 10, 15);

        assertEquals(72, weather.currentTempF());
        assertEquals(80, weather.maxTempF());
        assertEquals(55, weather.minTempF());
        assertEquals("Clear", weather.currentWeather());
        assertEquals(0.0, weather.currentPrecipMM());
        assertEquals(10, weather.currentWind());
        assertEquals(15, weather.gustWind());
    }

    @Test
    @DisplayName("Two Weather records with same data are equal")
    void equality() {
        Weather a = new Weather(72, 80, 55, "Clear", 0.0, 10, 15);
        Weather b = new Weather(72, 80, 55, "Clear", 0.0, 10, 15);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Weather records with different data are not equal")
    void inequality() {
        Weather a = new Weather(72, 80, 55, "Clear", 0.0, 10, 15);
        Weather b = new Weather(32, 40, 20, "Snow", 5.0, 25, 40);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Handles sub-zero temperatures")
    void subZeroTemperatures() {
        Weather weather = new Weather(-5, 10, -15, "Snow", 2.5, 20, 30);
        assertEquals(-5, weather.currentTempF());
        assertEquals(-15, weather.minTempF());
    }

    @Test
    @DisplayName("Handles zero wind conditions")
    void calmConditions() {
        Weather weather = new Weather(70, 75, 65, "Clear", 0.0, 0, 0);
        assertEquals(0, weather.currentWind());
        assertEquals(0, weather.gustWind());
    }

    @Test
    @DisplayName("Max temp is not validated to be greater than min temp (data comes from API)")
    void noTempValidation() {
        // This is valid — the record is a data container, not a validator
        Weather weather = new Weather(50, 40, 60, "Cloudy", 0.0, 5, 10);
        assertEquals(40, weather.maxTempF());
        assertEquals(60, weather.minTempF());
    }

    @Test
    @DisplayName("Weather condition string for rain detection")
    void rainConditionString() {
        Weather rain = new Weather(55, 60, 50, "Rain", 5.0, 15, 20);
        assertTrue(rain.currentWeather().toLowerCase().contains("rain"));
    }

    @Test
    @DisplayName("Weather condition string for snow detection")
    void snowConditionString() {
        Weather snow = new Weather(28, 32, 20, "Snow", 8.0, 10, 25);
        assertTrue(snow.currentWeather().toLowerCase().contains("snow"));
    }
}
