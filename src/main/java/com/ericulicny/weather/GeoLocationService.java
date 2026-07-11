package com.ericulicny.weather;

import com.ericulicny.domain.GeoLocation;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the device's geographic location (latitude, longitude, city, state)
 * via IP-based geolocation using the free ipapi.co service over HTTPS.
 */
public class GeoLocationService {

    private static final Logger logger = Logger.getLogger(GeoLocationService.class.getName());
    private static final String GEO_URL = "https://ipapi.co/json/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public GeoLocationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Attempts to determine the current geographic location from the public IP
     * address.
     *
     * @return the resolved location, or empty if the request failed
     */
    public Optional<GeoLocation> detect() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEO_URL))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warning("Geolocation API error: HTTP " + response.statusCode());
                return Optional.empty();
            }

            JSONObject obj = new JSONObject(response.body());
            double lat = obj.getDouble("latitude");
            double lon = obj.getDouble("longitude");
            String city = obj.optString("city", "Unknown");
            String state = obj.optString("region", "Unknown");

            GeoLocation geo = new GeoLocation(lat, lon, city, state);
            logger.info(String.format("Geolocation resolved: %s, %s (%.4f, %.4f)", city, state, lat, lon));
            return Optional.of(geo);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to resolve geolocation", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
