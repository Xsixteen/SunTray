package com.ericulicny.sun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the solar position calculator.
 *
 * Reference data validated against:
 * <a href="http://williams.best.vwh.net/sunrise_sunset_example.htm">
 * williams.best.vwh.net example</a> and NOAA Solar Calculator.
 */
class SunTest {

    // Clinton Township, MI
    private static final Location CLINTON_TWP = new Location(42.5869, -82.9200);

    // Use a fixed-offset zone for deterministic tests (EDT = UTC-4)
    private static final ZoneId EDT = ZoneId.of("America/Detroit");

    // Tolerance: within 5 minutes of expected time (algorithm uses approximations)
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    // ── Core calculation tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Sunrise/Sunset calculation accuracy")
    class CalculationAccuracy {

        @Test
        @DisplayName("Summer solstice — longest day of the year")
        void summerSolstice2026() {
            Sun sun = new Sun(CLINTON_TWP, EDT);
            sun.calculate(LocalDate.of(2026, 6, 21));

            LocalTime sunrise = sun.getSunriseTime();
            LocalTime sunset = sun.getSunsetTime();

            // Expected approx: sunrise ~5:54 AM, sunset ~9:14 PM EDT
            assertTimeNear(LocalTime.of(5, 54), sunrise, TOLERANCE, "Summer sunrise");
            assertTimeNear(LocalTime.of(21, 14), sunset, TOLERANCE, "Summer sunset");

            // Day length should be ~15 hours
            Duration dayLength = Duration.between(sunrise, sunset);
            assertTrue(dayLength.toHours() >= 14, "Summer day should be at least 14 hours");
            assertTrue(dayLength.toHours() <= 16, "Summer day should be at most 16 hours");
        }

        @Test
        @DisplayName("Winter solstice — shortest day of the year")
        void winterSolstice2026() {
            Sun sun = new Sun(CLINTON_TWP, EDT);
            sun.calculate(LocalDate.of(2026, 12, 21));

            LocalTime sunrise = sun.getSunriseTime();
            LocalTime sunset = sun.getSunsetTime();

            // Expected approx: sunrise ~7:58 AM, sunset ~5:05 PM EST
            assertTimeNear(LocalTime.of(7, 58), sunrise, TOLERANCE, "Winter sunrise");
            assertTimeNear(LocalTime.of(17, 5), sunset, TOLERANCE, "Winter sunset");

            // Day length should be ~9 hours
            Duration dayLength = Duration.between(sunrise, sunset);
            assertTrue(dayLength.toHours() >= 8, "Winter day should be at least 8 hours");
            assertTrue(dayLength.toHours() <= 10, "Winter day should be at most 10 hours");
        }

        @Test
        @DisplayName("Spring equinox — roughly equal day and night")
        void springEquinox2026() {
            Sun sun = new Sun(CLINTON_TWP, EDT);
            sun.calculate(LocalDate.of(2026, 3, 21));

            Duration dayLength = Duration.between(sun.getSunriseTime(), sun.getSunsetTime());
            long dayHours = dayLength.toHours();

            // Around equinox, day length should be close to 12 hours
            assertTrue(dayHours >= 11 && dayHours <= 13,
                    "Equinox day should be ~12 hours, was " + dayHours);
        }

        @Test
        @DisplayName("Known reference date — June 9, 2013 at lat 42.5869, lon -82.92")
        void knownReferenceDate() {
            // This is the original test case from the old Sun.java main() method
            Sun sun = new Sun(42.5869, -82.9200, EDT);
            sun.calculate(LocalDate.of(2013, 6, 9));

            assertNotNull(sun.getSunriseTime(), "Sunrise should be calculated");
            assertNotNull(sun.getSunsetTime(), "Sunset should be calculated");

            // Sunrise should be in the morning, sunset in the evening
            assertTrue(sun.getSunriseTime().isBefore(LocalTime.NOON), "Sunrise should be before noon");
            assertTrue(sun.getSunsetTime().isAfter(LocalTime.NOON), "Sunset should be after noon");
        }
    }

    // ── Invariant tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Sun calculation invariants")
    class Invariants {

        @Test
        @DisplayName("Sunrise always occurs before sunset")
        void sunriseBeforeSunset() {
            Sun sun = new Sun(CLINTON_TWP, EDT);

            // Test across several dates throughout the year
            for (int month = 1; month <= 12; month++) {
                LocalDate date = LocalDate.of(2026, month, 15);
                sun.calculate(date);

                assertTrue(sun.getSunriseTime().isBefore(sun.getSunsetTime()),
                        "Sunrise should be before sunset on " + date);
            }
        }

        @Test
        @DisplayName("Summer days are longer than winter days")
        void summerLongerThanWinter() {
            Sun summer = new Sun(CLINTON_TWP, EDT);
            summer.calculate(LocalDate.of(2026, 6, 21));
            Duration summerDay = Duration.between(summer.getSunriseTime(), summer.getSunsetTime());

            Sun winter = new Sun(CLINTON_TWP, EDT);
            winter.calculate(LocalDate.of(2026, 12, 21));
            Duration winterDay = Duration.between(winter.getSunriseTime(), winter.getSunsetTime());

            assertTrue(summerDay.compareTo(winterDay) > 0,
                    "Summer day (" + summerDay + ") should be longer than winter day (" + winterDay + ")");
        }

        @Test
        @DisplayName("Day length changes gradually between consecutive days")
        void gradualDayLengthChange() {
            Sun sun = new Sun(CLINTON_TWP, EDT);

            LocalDate date = LocalDate.of(2026, 4, 15);
            sun.calculate(date);
            Duration previousDayLength = Duration.between(sun.getSunriseTime(), sun.getSunsetTime());

            for (int i = 1; i <= 10; i++) {
                LocalDate nextDate = date.plusDays(i);
                Sun nextSun = new Sun(CLINTON_TWP, EDT);
                nextSun.calculate(nextDate);
                Duration currentDayLength = Duration.between(nextSun.getSunriseTime(), nextSun.getSunsetTime());

                // Day length should not change by more than 10 minutes day-to-day
                long diffMinutes = Math.abs(currentDayLength.toMinutes() - previousDayLength.toMinutes());
                assertTrue(diffMinutes <= 10,
                        "Day length should not change by more than 10 min between " + nextDate.minusDays(1)
                                + " and " + nextDate + " (was " + diffMinutes + " min)");

                previousDayLength = currentDayLength;
            }
        }
    }

    // ── DateTime accessor tests ─────────────────────────────────────────

    @Nested
    @DisplayName("DateTime accessors")
    class DateTimeAccessors {

        @Test
        @DisplayName("getSunsetDateTime combines date and time correctly")
        void sunsetDateTimeCorrect() {
            Sun sun = new Sun(CLINTON_TWP, EDT);
            LocalDate date = LocalDate.of(2026, 7, 4);
            sun.calculate(date);

            assertEquals(date, sun.getSunsetDateTime().toLocalDate(),
                    "Sunset date should match the calculated date");
            assertEquals(sun.getSunsetTime(), sun.getSunsetDateTime().toLocalTime(),
                    "Sunset time components should match");
        }

        @Test
        @DisplayName("getSunriseDateTime combines date and time correctly")
        void sunriseDateTimeCorrect() {
            Sun sun = new Sun(CLINTON_TWP, EDT);
            LocalDate date = LocalDate.of(2026, 1, 15);
            sun.calculate(date);

            assertEquals(date, sun.getSunriseDateTime().toLocalDate(),
                    "Sunrise date should match the calculated date");
            assertEquals(sun.getSunriseTime(), sun.getSunriseDateTime().toLocalTime(),
                    "Sunrise time components should match");
        }
    }

    // ── Location / constructor tests ────────────────────────────────────

    @Nested
    @DisplayName("Constructor variants")
    class ConstructorTests {

        @Test
        @DisplayName("Location record constructor produces same results as double constructor")
        void locationConstructorEquivalence() {
            Sun fromLocation = new Sun(CLINTON_TWP, EDT);
            Sun fromDoubles = new Sun(42.5869, -82.9200, EDT);

            LocalDate date = LocalDate.of(2026, 6, 1);
            fromLocation.calculate(date);
            fromDoubles.calculate(date);

            assertEquals(fromLocation.getSunriseTime(), fromDoubles.getSunriseTime(),
                    "Sunrise should match between constructors");
            assertEquals(fromLocation.getSunsetTime(), fromDoubles.getSunsetTime(),
                    "Sunset should match between constructors");
        }

        @Test
        @DisplayName("Different locations produce different sunrise/sunset times")
        void differentLocations() {
            // New York (further east) should have earlier sunrise than Clinton Twp
            Location newYork = new Location(40.7128, -74.0060);
            ZoneId eastern = ZoneId.of("America/New_York");

            Sun clintonSun = new Sun(CLINTON_TWP, EDT);
            Sun nySun = new Sun(newYork, eastern);

            LocalDate date = LocalDate.of(2026, 6, 15);
            clintonSun.calculate(date);
            nySun.calculate(date);

            // At similar latitudes, the more eastern location should see sunrise earlier
            assertTrue(nySun.getSunriseTime().isBefore(clintonSun.getSunriseTime()),
                    "NYC sunrise (" + nySun.getSunriseTime() + ") should be before Clinton Twp ("
                            + clintonSun.getSunriseTime() + ")");
        }
    }

    // ── DST handling tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("DST handling")
    class DstTests {

        @Test
        @DisplayName("Sunset is later in summer (EDT) than same date's UTC offset suggests")
        void dstAffectsSunset() {
            // Use America/Detroit which observes DST
            Sun dstSun = new Sun(CLINTON_TWP, ZoneId.of("America/Detroit"));

            // Summer date (EDT, UTC-4)
            dstSun.calculate(LocalDate.of(2026, 7, 1));
            LocalTime summerSunset = dstSun.getSunsetTime();

            // Winter date (EST, UTC-5) 
            dstSun.calculate(LocalDate.of(2026, 1, 1));
            LocalTime winterSunset = dstSun.getSunsetTime();

            // Summer sunset should be significantly later than winter sunset
            assertTrue(summerSunset.isAfter(winterSunset),
                    "Summer sunset (" + summerSunset + ") should be after winter sunset (" + winterSunset + ")");
        }

        @Test
        @DisplayName("Sunrise times are reasonable for the time zone")
        void sunriseInReasonableRange() {
            Sun sun = new Sun(CLINTON_TWP, EDT);

            // Check a few dates across the year
            for (int month = 1; month <= 12; month++) {
                sun.calculate(LocalDate.of(2026, month, 15));
                LocalTime sunrise = sun.getSunriseTime();

                // Sunrise at this latitude should always be between 4:00 AM and 9:00 AM
                assertTrue(sunrise.isAfter(LocalTime.of(4, 0)) && sunrise.isBefore(LocalTime.of(9, 0)),
                        "Sunrise on month " + month + " should be between 4:00-9:00 AM, was " + sunrise);
            }
        }
    }

    // ── Test utilities ──────────────────────────────────────────────────

    private static void assertTimeNear(LocalTime expected, LocalTime actual, Duration tolerance, String message) {
        long diffSeconds = Math.abs(Duration.between(expected, actual).getSeconds());
        assertTrue(diffSeconds <= tolerance.getSeconds(),
                message + ": expected ~" + expected + " but was " + actual
                        + " (diff: " + (diffSeconds / 60) + " minutes)");
    }
}
