package com.ericulicny.domain;

/**
 * Immutable snapshot of current weather conditions.
 */
public record Weather(
        long currentTempF,
        long maxTempF,
        long minTempF,
        String currentWeather,
        double currentPrecipMM,
        int currentWind,
        int gustWind
) {}
