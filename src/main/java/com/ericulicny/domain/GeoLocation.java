package com.ericulicny.domain;

import com.ericulicny.sun.Location;

/**
 * Geographic location with city/state metadata resolved from IP geolocation.
 */
public record GeoLocation(
        double latitude,
        double longitude,
        String city,
        String state
) {
    /** Converts to a {@link Location} for sun-calculation use. */
    public Location toLocation() {
        return new Location(latitude, longitude);
    }
}
