package com.ericulicny.weather;

import com.ericulicny.domain.Weather;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WeatherService.
 * <p>
 * Integration tests that hit the real API are guarded by the presence of
 * the SUNTRAY_WEATHER_API_KEY environment variable.
 */
class WeatherServiceTest {

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Returns empty when API key is invalid")
        void invalidApiKeyReturnsEmpty() {
            WeatherService service = new WeatherService("invalid-key-12345");
            Optional<Weather> result = service.getTodaysWeather("berkley");

            assertTrue(result.isEmpty(), "Should return empty Optional for invalid API key");
        }

        @Test
        @DisplayName("Returns empty when API key is blank")
        void blankApiKeyReturnsEmpty() {
            WeatherService service = new WeatherService("");
            Optional<Weather> result = service.getTodaysWeather("berkley");

            assertTrue(result.isEmpty(), "Should return empty Optional for blank API key");
        }

        @Test
        @DisplayName("Returns empty for nonsense city name")
        void nonsenseCityReturnsEmpty() {
            WeatherService service = new WeatherService("fake-key");
            Optional<Weather> result = service.getTodaysWeather("xyznonexistentcity12345");

            assertTrue(result.isEmpty(), "Should return empty Optional for nonexistent city");
        }

        @Test
        @DisplayName("Does not throw exceptions on failure")
        void noExceptionsOnFailure() {
            WeatherService service = new WeatherService("");
            assertDoesNotThrow(() -> service.getTodaysWeather("berkley"),
                    "Should handle errors gracefully without throwing");
        }
    }

    @Nested
    @DisplayName("Live API integration")
    @EnabledIfEnvironmentVariable(named = "SUNTRAY_WEATHER_API_KEY", matches = ".+")
    class LiveApiTests {

        private final WeatherService service = new WeatherService(
                System.getenv("SUNTRAY_WEATHER_API_KEY"));

        @Test
        @DisplayName("Fetches weather for a valid city")
        void fetchValidCity() {
            Optional<Weather> result = service.getTodaysWeather("berkley");

            assertTrue(result.isPresent(), "Should return weather data for valid city");

            Weather weather = result.get();
            assertNotNull(weather.currentWeather(), "Current weather should not be null");
            assertFalse(weather.currentWeather().isBlank(), "Current weather should not be blank");
        }

        @Test
        @DisplayName("Temperature values are in a reasonable Fahrenheit range")
        void temperatureRange() {
            Optional<Weather> result = service.getTodaysWeather("berkley");
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
            Optional<Weather> result = service.getTodaysWeather("berkley");
            assertTrue(result.isPresent());

            Weather weather = result.get();
            assertTrue(weather.currentWind() >= 0, "Wind speed should be non-negative");
        }

        @Test
        @DisplayName("Can fetch weather for multiple cities")
        void multipleCities() {
            String[] cities = {"New York", "London", "Tokyo"};
            for (String city : cities) {
                Optional<Weather> result = service.getTodaysWeather(city);
                assertTrue(result.isPresent(), "Should return weather for " + city);
            }
        }
    }
}
