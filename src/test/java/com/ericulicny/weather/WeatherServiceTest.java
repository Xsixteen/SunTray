package com.ericulicny.weather;

import com.ericulicny.domain.Weather;
import com.ericulicny.sun.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WeatherService.
 */
class WeatherServiceTest {

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Returns empty for out-of-bounds coordinates")
        void invalidCoordinatesReturnEmpty() {
            WeatherService service = new WeatherService();
            // Invalid latitude (must be -90 to 90)
            Optional<Weather> result = service.getTodaysWeather(new Location(100.0, 0.0));

            assertTrue(result.isEmpty(), "Should return empty Optional for invalid coordinates");
        }

        @Test
        @DisplayName("Does not throw exceptions on failure")
        void noExceptionsOnFailure() {
            WeatherService service = new WeatherService();
            assertDoesNotThrow(() -> service.getTodaysWeather(new Location(100.0, 0.0)),
                    "Should handle errors gracefully without throwing");
        }
    }

    @Nested
    @DisplayName("Live API integration")
    class LiveApiTests {

        private final WeatherService service = new WeatherService();
        // Berkley, MI coordinates
        private final Location berkleyLocation = new Location(42.5031, -83.1835);

        @Test
        @DisplayName("Fetches weather for valid coordinates")
        void fetchValidCoordinates() {
            Optional<Weather> result = service.getTodaysWeather(berkleyLocation);

            assertTrue(result.isPresent(), "Should return weather data for valid coordinates");

            Weather weather = result.get();
            assertNotNull(weather.currentWeather(), "Current weather should not be null");
            assertFalse(weather.currentWeather().isBlank(), "Current weather should not be blank");
        }

        @Test
        @DisplayName("Temperature values are in a reasonable Fahrenheit range")
        void temperatureRange() {
            Optional<Weather> result = service.getTodaysWeather(berkleyLocation);
            assertTrue(result.isPresent());

            Weather weather = result.get();
            // Fahrenheit temps should be between -60 and 140 for any Earth location
            assertTrue(weather.currentTempF() > -60 && weather.currentTempF() < 140,
                    "Current temp " + weather.currentTempF() + "°F out of reasonable range");
            assertTrue(weather.maxTempF() >= weather.minTempF(),
                    "Max temp should be >= min temp");
        }

        @Test
        @DisplayName("Wind speed is non-negative")
        void windSpeedNonNegative() {
            Optional<Weather> result = service.getTodaysWeather(berkleyLocation);
            assertTrue(result.isPresent());

            Weather weather = result.get();
            assertTrue(weather.currentWind() >= 0, "Wind speed should be non-negative");
        }

        @Test
        @DisplayName("Can fetch weather for multiple locations")
        void multipleLocations() {
            Location[] locations = {
                    new Location(40.7128, -74.0060), // New York
                    new Location(51.5074, -0.1278),  // London
                    new Location(35.6762, 139.6503)  // Tokyo
            };
            
            for (Location loc : locations) {
                Optional<Weather> result = service.getTodaysWeather(loc);
                assertTrue(result.isPresent(), "Should return weather for location: " + loc);
            }
        }
    }
}
