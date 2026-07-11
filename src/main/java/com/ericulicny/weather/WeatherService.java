package com.ericulicny.weather;

import com.ericulicny.domain.Weather;
import com.ericulicny.sun.Location;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches current weather data from the Open-Meteo API.
 */
public class WeatherService {

    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Fetches the current weather for the given location.
     *
     * @param location the location to query
     * @return the weather data, or empty if the request failed
     */
    public Optional<Weather> getTodaysWeather(Location location) {
        try {
            String urlString = String.format("%s?latitude=%f&longitude=%f" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,wind_gusts_10m,precipitation" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max" +
                    "&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=mm&timezone=auto",
                    BASE_URL, location.latitude(), location.longitude());

            URI uri = URI.create(urlString);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Weather API returned status " + response.statusCode());

            if (response.statusCode() != 200) {
                logger.warning("Weather API error: HTTP " + response.statusCode());
                return Optional.empty();
            }

            JSONObject obj = new JSONObject(response.body());
            
            JSONObject current = obj.getJSONObject("current");
            double currentTemp = current.getDouble("temperature_2m");
            int weatherCode = current.getInt("weather_code");
            double windSpeed = current.getDouble("wind_speed_10m");
            double precipitation = current.getDouble("precipitation");
            
            int gustWind = 0;
            try {
                if (!current.isNull("wind_gusts_10m")) {
                    gustWind = (int) Math.round(current.getDouble("wind_gusts_10m"));
                }
            } catch (JSONException ignored) {
                // Gust data is optional
            }

            JSONObject daily = obj.getJSONObject("daily");
            double maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0);
            double precipSumMM = daily.getJSONArray("precipitation_sum").getDouble(0);
            double precipSumInches = precipSumMM / 25.4;
            int precipProbability = daily.getJSONArray("precipitation_probability_max").getInt(0);

            String conditions = mapWeatherCodeToCondition(weatherCode);

            Weather weather = new Weather(
                    Math.round(currentTemp),
                    Math.round(maxTemp),
                    Math.round(minTemp),
                    conditions,
                    precipitation,
                    (int) Math.round(windSpeed),
                    gustWind,
                    precipSumInches,
                    precipProbability
            );

            logger.info(String.format("Weather: current=%d°F, max=%d°F, min=%d°F, conditions=%s",
                    weather.currentTempF(), weather.maxTempF(), weather.minTempF(), weather.currentWeather()));

            return Optional.of(weather);

        } catch (IOException | InterruptedException | JSONException e) {
            logger.log(Level.WARNING, "Failed to fetch weather data", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static String mapWeatherCodeToCondition(int code) {
        if (code == 0) return "Clear";
        if (code == 1 || code == 2 || code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain Showers";
        if (code >= 85 && code <= 86) return "Snow Showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Unknown";
    }
}
