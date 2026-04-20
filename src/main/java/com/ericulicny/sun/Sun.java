package com.ericulicny.sun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

/**
 * Solar position calculator for sunrise and sunset times.
 * <p>
 * Based on the algorithm from: <a href="http://williams.best.vwh.net/sunrise_sunset_algorithm.htm">
 * williams.best.vwh.net</a>
 * <p>
 * Validated against: <a href="http://williams.best.vwh.net/sunrise_sunset_example.htm">
 * williams.best.vwh.net example</a>
 */
public class Sun {

    private static final Logger logger = Logger.getLogger(Sun.class.getName());
    private static final double ZENITH = 90.833;

    /** Whether to calculate for sunrise or sunset. */
    public enum SunEvent { SUNRISE, SUNSET }

    private final double longitude;
    private final double latitude;
    private final ZoneId zoneId;

    private LocalDate date;
    private LocalTime sunsetTime;
    private LocalTime sunriseTime;

    public Sun(Location location, ZoneId zoneId) {
        this.latitude = location.latitude();
        this.longitude = location.longitude();
        this.zoneId = zoneId;
    }

    public Sun(double lat, double lon, ZoneId zoneId) {
        this.latitude = lat;
        this.longitude = lon;
        this.zoneId = zoneId;
    }

    /**
     * Sets the date and calculates sunrise/sunset times.
     */
    public void calculate(LocalDate date) {
        this.date = date;

        int dayOfYear = dayOfTheYear(date.getMonthValue(), date.getDayOfMonth(), date.getYear());

        // Calculate UTC offset accounting for DST automatically
        ZonedDateTime zdt = date.atStartOfDay(zoneId);
        int utcOffsetHours = zdt.getOffset().getTotalSeconds() / 3600;

        // Sunset calculation
        double ssLatToHour = convLatToHour(dayOfYear, SunEvent.SUNSET);
        double ssMeanAnomaly = meanAnomaly(ssLatToHour);
        double ssTrueLong = calcTrueLong(ssMeanAnomaly);
        double ssRA = calcRightAscension(ssTrueLong);
        double ssLocalHour = sunsLocalHour(ssTrueLong, SunEvent.SUNSET);
        double ssMeanTime = meanTimeOfSetting(ssRA, ssLocalHour, ssLatToHour);
        double ssUtc = convToUTC(ssMeanTime);
        double sunset = ssUtc + utcOffsetHours;

        // Sunrise calculation
        double srLatToHour = convLatToHour(dayOfYear, SunEvent.SUNRISE);
        double srMeanAnomaly = meanAnomaly(srLatToHour);
        double srTrueLong = calcTrueLong(srMeanAnomaly);
        double srRA = calcRightAscension(srTrueLong);
        double srLocalHour = sunsLocalHour(srTrueLong, SunEvent.SUNRISE);
        double srMeanTime = meanTimeOfSetting(srRA, srLocalHour, srLatToHour);
        double srUtc = convToUTC(srMeanTime);
        double sunrise = srUtc + utcOffsetHours;

        this.sunsetTime = decimalHoursToLocalTime(sunset);
        this.sunriseTime = decimalHoursToLocalTime(sunrise);

        logger.fine(() -> String.format("Calculated for %s: sunrise=%s, sunset=%s", date, sunriseTime, sunsetTime));
    }

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getSunriseTime() {
        return sunriseTime;
    }

    public LocalDateTime getSunsetDateTime() {
        return LocalDateTime.of(date, sunsetTime);
    }

    public LocalDateTime getSunriseDateTime() {
        return LocalDateTime.of(date, sunriseTime);
    }

    // ── Private calculation methods ──────────────────────────────────────

    private static LocalTime decimalHoursToLocalTime(double decimalHours) {
        // Normalize to 0-24 range
        while (decimalHours < 0) decimalHours += 24;
        while (decimalHours >= 24) decimalHours -= 24;

        int hour = (int) decimalHours;
        int minute = (int) ((decimalHours - hour) * 60);
        int second = (int) (((decimalHours - hour) * 3600) - minute * 60);
        return LocalTime.of(hour, minute, second);
    }

    private static int dayOfTheYear(int month, int day, int year) {
        int n1 = (int) Math.floor(275.0 * month / 9);
        int n2 = (int) Math.floor((month + 9.0) / 12);
        int n3 = (int) (1 + Math.floor((year - 4.0 * Math.floor(year / 4.0) + 2) / 3));
        return n1 - (n2 * n3) + day - 30;
    }

    private double convLatToHour(int dayOfYear, SunEvent event) {
        double hourAngle = (event == SunEvent.SUNSET) ? 18.0 : 6.0;
        return dayOfYear + ((hourAngle - (longitude / 15)) / 24);
    }

    private static double meanAnomaly(double latToHour) {
        return (0.9856 * latToHour) - 3.289;
    }

    private static double calcTrueLong(double meanAnomaly) {
        double l = meanAnomaly
                + (1.916 * Math.sin(Math.toRadians(meanAnomaly)))
                + (0.020 * Math.sin(Math.toRadians(2 * meanAnomaly)))
                + 282.634;

        // Normalize to [0, 360)
        if (l > 360.0) l -= 360;
        else if (l < 0) l += 360;
        return l;
    }

    private static double calcRightAscension(double trueLong) {
        double ra = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(trueLong))));

        // Normalize to [0, 360)
        if (ra > 360.0) ra -= 360;
        else if (ra < 0) ra += 360;

        // Adjust quadrant
        double lQuadrant = Math.floor(trueLong / 90) * 90;
        double raQuadrant = Math.floor(ra / 90) * 90;
        ra += (lQuadrant - raQuadrant);

        return ra / 15; // Convert to hours
    }

    private double sunsLocalHour(double trueLong, SunEvent event) {
        double sinDec = 0.39782 * Math.sin(Math.toRadians(trueLong));
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos(Math.toRadians(ZENITH))
                - (sinDec * Math.sin(Math.toRadians(latitude))))
                / (cosDec * Math.cos(Math.toRadians(latitude)));

        double localHour = Math.toDegrees(Math.acos(cosH));
        if (event == SunEvent.SUNRISE) {
            localHour = 360 - localHour;
        }
        return localHour / 15;
    }

    private static double meanTimeOfSetting(double rightAscension, double localHour, double latToHour) {
        return localHour + rightAscension - (0.06571 * latToHour) - 6.622;
    }

    private double convToUTC(double meanTime) {
        double utc = meanTime - (longitude / 15);
        if (utc > 24) utc -= 24;
        else if (utc < 0) utc += 24;
        return utc;
    }
}