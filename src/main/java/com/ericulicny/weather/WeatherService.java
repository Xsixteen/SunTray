package com.ericulicny.weather;

import com.ericulicny.domain.Weather;
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
 * Fetches current weather data from the OpenWeatherMap API.
 */
public class WeatherService {

    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String apiKey;

    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Fetches the current weather for the given city.
     *
     * @param city the city name to query
     * @return the weather data, or empty if the request failed
     */
    public Optional<Weather> getTodaysWeather(String city) {
        try {
            URI uri = URI.create(BASE_URL + "?q=" + java.net.URLEncoder.encode(city, java.nio.charset.StandardCharsets.UTF_8) + "&appid=" + apiKey);
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
            double currentTempK = obj.getJSONObject("main").getDouble("temp");
            double maxTempK = obj.getJSONObject("main").getDouble("temp_max");
            double minTempK = obj.getJSONObject("main").getDouble("temp_min");
            double windSpeed = obj.getJSONObject("wind").getDouble("speed");

            int gustWind = 0;
            try {
                gustWind = (int) Math.round(windMeterToMph(obj.getJSONObject("wind").getDouble("gust")));
            } catch (JSONException ignored) {
                // Gust data is optional in the API response
            }

            String conditions = obj.getJSONArray("weather").getJSONObject(0).getString("main");

            Weather weather = new Weather(
                    convKtoF(currentTempK),
                    convKtoF(maxTempK),
                    convKtoF(minTempK),
                    conditions,
                    0.0, // precipitation not available in basic endpoint
                    (int) Math.round(windMeterToMph(windSpeed)),
                    gustWind
            );

            logger.info(String.format("Weather: current=%d°F, max=%d°F, min=%d°F, conditions=%s",
                    weather.currentTempF(), weather.maxTempF(), weather.minTempF(), weather.currentWeather()));

            return Optional.of(weather);

        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Failed to fetch weather data", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static long convKtoF(double kelvin) {
        return Math.round((kelvin - 273.15) * 9.0 / 5.0 + 32);
    }

    private static double windMeterToMph(double windSpeed) {
        return windSpeed * 2.23694;
    }
}
