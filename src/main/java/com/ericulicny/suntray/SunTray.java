package com.ericulicny.suntray;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import com.ericulicny.domain.GeoLocation;
import com.ericulicny.domain.Weather;
import com.ericulicny.sun.Location;
import com.ericulicny.sun.Sun;
import com.ericulicny.weather.GeoLocationService;
import com.ericulicny.weather.WeatherService;

/**
 * A macOS system tray application displaying sunrise/sunset times,
 * seasonal countdowns, and current weather conditions.
 */
public class SunTray {

    private static final Logger logger = Logger.getLogger(SunTray.class.getName());
    private static final String VERSION = "v1.0";
    private static final int UPDATE_INTERVAL_MS = 30 * 60_000; // 30 minutes
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mma");
    private static final DateTimeFormatter TIME_FORMAT_FULL = DateTimeFormatter.ofPattern("h:mma MMM dd yyyy");

    // Seasonal event dates (month-day)
    private static final MonthDay SPRING_EQUINOX = MonthDay.of(3, 21);
    private static final MonthDay SUMMER_SOLSTICE = MonthDay.of(6, 21);
    private static final MonthDay FALL_EQUINOX = MonthDay.of(9, 21);
    private static final MonthDay WINTER_SOLSTICE = MonthDay.of(12, 21);

    // Configuration
    private final Location location;
    private final WeatherService weatherService;

    // Tray icon images
    private final Image sunIcon = loadIcon("sun-icon-md.png");
    private final Image sunPlusIcon = loadIcon("sun-plus.png");
    private final Image sunMinusIcon = loadIcon("sun-minus.png");
    private final Image moonIcon = loadIcon("full-moon-icon-md.png");
    private final Image snowIcon = loadIcon("snowflake-icon.png");
    private final Image rainIcon = loadIcon("raindrop-icon.png");

    // System tray
    private TrayIcon trayIcon;

    // Current state
    private LocalTime sunsetTime;
    private LocalTime sunriseTime;
    private boolean positiveTimeDelta;

    // Menu items — location
    private final MenuItem locationItem = createDisabledItem("Location: ");

    // Menu items — sun data
    private final MenuItem sunsetItem = createDisabledItem("Sunset: ");
    private final MenuItem sunriseItem = createDisabledItem("Sunrise: ");
    private final MenuItem timeToItem = createDisabledItem("Time until Sunset: ");
    private final MenuItem tomorrowDeltaItem = createDisabledItem("Tomorrow change: ");
    private final MenuItem dayLengthItem = createDisabledItem("Today's Day Length: ");

    // Menu items — seasonal countdowns
    private final MenuItem springItem = createDisabledItem("Days until Spring Equinox: ");
    private final MenuItem summerItem = createDisabledItem("Days until Summer Solstice: ");
    private final MenuItem fallItem = createDisabledItem("Days until Fall Equinox: ");
    private final MenuItem winterItem = createDisabledItem("Days until Winter Solstice: ");

    // Menu items — max/min day lengths
    private final MenuItem maxDayItem = createDisabledItem("Maximum Day Length: ");
    private final MenuItem minDayItem = createDisabledItem("Minimum Day Length: ");

    // Menu items — weather
    private final MenuItem currentConditionsItem = createDisabledItem("Current Weather: ");
    private final MenuItem currentTempItem = createDisabledItem("Current Temp: ");
    private final MenuItem maxTempItem = createDisabledItem("High Temp: ");
    private final MenuItem minTempItem = createDisabledItem("Low Temp: ");
    private final MenuItem windSpeedItem = createDisabledItem("Wind Speed: ");
    private final MenuItem precipSumItem = createDisabledItem("Precipitation Sum: ");
    private final MenuItem precipProbItem = createDisabledItem("Precipitation Probability: ");
    private final MenuItem weatherLastUpdatedItem = createDisabledItem("Weather Last Updated: ");

    // ── Entry point ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Hide the Dock icon — must be set before AWT toolkit initializes
        System.setProperty("apple.awt.UIElement", "true");

        // Try IP-based geolocation first, fall back to env vars
        GeoLocationService geoService = new GeoLocationService();
        Optional<GeoLocation> detected = geoService.detect();

        Location location;
        String cityState;
        if (detected.isPresent()) {
            GeoLocation geo = detected.get();
            location = geo.toLocation();
            cityState = geo.city() + ", " + geo.state();
        } else {
            double lat = parseEnvDouble("SUNTRAY_LATITUDE", 42.5031);
            double lon = parseEnvDouble("SUNTRAY_LONGITUDE", -83.1835);
            location = new Location(lat, lon);
            cityState = "Unknown";
        }

        SunTray app = new SunTray(location, cityState);
        app.doUpdate();

        Timer pulse = new Timer(UPDATE_INTERVAL_MS, e -> app.doUpdate());
        pulse.start();
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public SunTray(Location location, String cityState) {
        this.location = location;
        this.weatherService = new WeatherService();
        locationItem.setLabel("\uD83D\uDCCD " + cityState);

        if (!SystemTray.isSupported()) {
            logger.severe("SystemTray is not supported on this platform");
            return;
        }

        trayIcon = new TrayIcon(sunIcon, "SunTray " + VERSION);
        trayIcon.setPopupMenu(buildMenu());

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "Failed to add tray icon", e);
        }
    }

    // ── Update cycle ────────────────────────────────────────────────────

    public void doUpdate() {
        try {
            LocalDate today = LocalDate.now();

            // Calculate sun times for today
            Sun currentSun = new Sun(location, ZONE);
            currentSun.calculate(today);

            this.sunsetTime = currentSun.getSunsetTime();
            this.sunriseTime = currentSun.getSunriseTime();

            // Update all menu sections
            updateSunTimes(currentSun, today);
            updateDeltaTime();
            updateTomorrowDelta(currentSun, today);
            updateDayLength(sunsetTime, sunriseTime, dayLengthItem, "Today's Day Length: ");
            updateSeasonalCountdowns(today);
            updateMaxMinDayLengths(today);

            // Fetch and display weather
            Optional<Weather> weather = weatherService.getTodaysWeather(location);
            weather.ifPresent(this::updateWeatherDisplay);

            // Update tray icon based on sun/weather state
            updateIcon(weather.orElse(null));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Update cycle failed", e);
        }
    }

    // ── Sun times ───────────────────────────────────────────────────────

    private void updateSunTimes(Sun currentSun, LocalDate today) {
        LocalDateTime sunsetDateTime = LocalDateTime.of(today, sunsetTime);
        LocalDateTime sunriseDateTime = LocalDateTime.of(today, sunriseTime);

        if (hasSunSet()) {
            sunsetItem.setLabel("Sunset occurred at: " + sunsetDateTime.format(TIME_FORMAT_FULL));
            updateTomorrowSunrise(today);
        } else {
            sunriseItem.setLabel("Sunrise: " + sunriseDateTime.format(TIME_FORMAT_FULL));
            sunsetItem.setLabel("Sunset: " + sunsetDateTime.format(TIME_FORMAT_FULL));
        }
    }

    private void updateDeltaTime() {
        LocalDateTime sunsetDateTime = LocalDateTime.of(LocalDate.now(), sunsetTime);
        Duration delta = Duration.between(LocalDateTime.now(), sunsetDateTime);

        long hours = Math.abs(delta.toHours());
        long minutes = Math.abs(delta.toMinutesPart());

        String prefix = hasSunSet() ? "Time since Sunset: " : "Time until Sunset: ";
        timeToItem.setLabel(prefix + hours + " hour & " + minutes + " minutes");
    }

    private void updateTomorrowSunrise(LocalDate today) {
        LocalDate tomorrow = today.plusDays(1);
        Sun tomorrowSun = new Sun(location, ZONE);
        tomorrowSun.calculate(tomorrow);
        sunriseItem.setLabel("Tomorrow's Sunrise: " + tomorrowSun.getSunriseTime().format(TIME_FORMAT));
    }

    // ── Tomorrow delta ──────────────────────────────────────────────────

    private void updateTomorrowDelta(Sun currentSun, LocalDate today) {
        LocalDate tomorrow = today.plusDays(1);
        Sun tomorrowSun = new Sun(location, ZONE);
        tomorrowSun.calculate(tomorrow);

        Duration todayLength = Duration.between(currentSun.getSunriseDateTime(), currentSun.getSunsetDateTime());
        Duration tomorrowLength = Duration.between(tomorrowSun.getSunriseDateTime(), tomorrowSun.getSunsetDateTime());
        Duration delta = tomorrowLength.minus(todayLength);
        positiveTimeDelta = !delta.isNegative();

        long minutes = delta.toMinutes();
        long seconds = Math.abs(delta.toSecondsPart());
        tomorrowDeltaItem.setLabel("Tomorrow Time Delta: " + minutes + " min & " + seconds + " seconds");
    }

    // ── Day length ──────────────────────────────────────────────────────

    private void updateDayLength(LocalTime sunset, LocalTime sunrise, MenuItem menuItem, String label) {
        Duration duration = Duration.between(sunrise, sunset);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        menuItem.setLabel(label + hours + " hours & " + minutes + " minutes");
    }

    private void updateMaxMinDayLengths(LocalDate today) {
        int year = today.getYear();

        // Summer solstice — longest day
        Sun summerSun = new Sun(location, ZONE);
        summerSun.calculate(LocalDate.of(year, 6, 21));
        updateDayLength(summerSun.getSunsetTime(), summerSun.getSunriseTime(), maxDayItem, "Maximum Day Length: ");

        // Winter solstice — shortest day
        Sun winterSun = new Sun(location, ZONE);
        winterSun.calculate(LocalDate.of(year, 12, 21));
        updateDayLength(winterSun.getSunsetTime(), winterSun.getSunriseTime(), minDayItem, "Minimum Day Length: ");
    }

    // ── Seasonal countdowns ─────────────────────────────────────────────

    private void updateSeasonalCountdowns(LocalDate today) {
        updateSeasonalItem(today, SPRING_EQUINOX, springItem, "Days until Spring Equinox: ");
        updateSeasonalItem(today, SUMMER_SOLSTICE, summerItem, "Days until Summer Solstice: ");
        updateSeasonalItem(today, FALL_EQUINOX, fallItem, "Days until Fall Equinox: ");
        updateSeasonalItem(today, WINTER_SOLSTICE, winterItem, "Days until Winter Solstice: ");
    }

    private void updateSeasonalItem(LocalDate today, MonthDay event, MenuItem menuItem, String label) {
        LocalDate eventDate = event.atYear(today.getYear());
        if (!today.isBefore(eventDate)) {
            eventDate = event.atYear(today.getYear() + 1);
        }
        long days = ChronoUnit.DAYS.between(today, eventDate);
        menuItem.setLabel(label + days + " Days");
    }

    // ── Weather ─────────────────────────────────────────────────────────

    private void updateWeatherDisplay(Weather weather) {
        currentConditionsItem.setLabel("Current Weather: " + weather.currentWeather());
        currentTempItem.setLabel("Current Temp: " + weather.currentTempF() + "\u00B0F");
        maxTempItem.setLabel("Max Temp: " + weather.maxTempF() + "\u00B0F");
        minTempItem.setLabel("Min Temp: " + weather.minTempF() + "\u00B0F");
        windSpeedItem.setLabel("Wind Speed: " + weather.currentWind() + " mph");
        precipSumItem.setLabel("Precipitation Sum: " + String.format("%.2f", weather.precipitationSumInches()) + " in");
        precipProbItem.setLabel("Precipitation Probability: " + weather.precipitationProbabilityPct() + "%");
        weatherLastUpdatedItem.setLabel("Weather Last Updated: " + LocalTime.now().format(TIME_FORMAT));
    }

    // ── Icon management ─────────────────────────────────────────────────

    private void updateIcon(Weather weather) {
        if (hasSunSet() || !hasSunRisen()) {
            trayIcon.setImage(moonIcon);
        } else {
            if (positiveTimeDelta) {
                trayIcon.setImage(sunPlusIcon);
            } else {
                trayIcon.setImage(sunMinusIcon);
            }
        }

        if (weather != null) {
            String conditions = weather.currentWeather().toLowerCase();
            if (conditions.contains("rain")) {
                trayIcon.setImage(rainIcon);
            } else if (conditions.contains("snow")) {
                trayIcon.setImage(snowIcon);
            }
        }
    }

    private boolean hasSunSet() {
        return LocalTime.now().isAfter(sunsetTime);
    }

    private boolean hasSunRisen() {
        return LocalTime.now().isAfter(sunriseTime);
    }

    // ── Menu construction ───────────────────────────────────────────────

    private PopupMenu buildMenu() {
        PopupMenu popup = new PopupMenu();

        popup.add(locationItem);
        popup.addSeparator();
        popup.add(sunsetItem);
        popup.add(sunriseItem);
        popup.add(dayLengthItem);
        popup.addSeparator();
        popup.add(timeToItem);
        popup.add(tomorrowDeltaItem);
        popup.addSeparator();
        popup.add(summerItem);
        popup.add(springItem);
        popup.add(winterItem);
        popup.add(fallItem);
        popup.addSeparator();
        popup.add(maxDayItem);
        popup.add(minDayItem);
        popup.addSeparator();
        popup.add(currentConditionsItem);
        popup.add(currentTempItem);
        popup.add(maxTempItem);
        popup.add(minTempItem);
        popup.add(windSpeedItem);
        popup.add(precipSumItem);
        popup.add(precipProbItem);
        popup.add(weatherLastUpdatedItem);
        popup.addSeparator();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(exitItem);

        return popup;
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private static MenuItem createDisabledItem(String label) {
        MenuItem item = new MenuItem(label);
        item.setEnabled(false);
        return item;
    }

    private Image loadIcon(String resourceName) {
        return Toolkit.getDefaultToolkit().getImage(
                getClass().getClassLoader().getResource(resourceName));
    }

    private static double parseEnvDouble(String envVar, double defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for " + envVar + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }
}
