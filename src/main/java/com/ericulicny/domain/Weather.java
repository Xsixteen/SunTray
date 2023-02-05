package com.ericulicny.domain;

public class Weather {
    private Long currentTempF;
    private Long maxTempF;
    private Long minTempF;
    private String currentWeather;
    private Double currentPrecipMM;
    private Integer currentWind;
    private Integer gustWind;

    public Long getCurrentTempF() {
        return currentTempF;
    }

    public void setCurrentTempF(Long currentTempF) {
        this.currentTempF = currentTempF;
    }

    public Long getMaxTempF() {
        return maxTempF;
    }

    public void setMaxTempF(Long maxTempF) {
        this.maxTempF = maxTempF;
    }

    public Long getMinTempF() {
        return minTempF;
    }

    public void setMinTempF(Long minTempF) {
        this.minTempF = minTempF;
    }

    public String getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(String currentWeather) {
        this.currentWeather = currentWeather;
    }

    public Double getCurrentPrecipMM() {
        return currentPrecipMM;
    }

    public void setCurrentPrecipMM(Double currentPrecipMM) {
        this.currentPrecipMM = currentPrecipMM;
    }

    public Integer getCurrentWind() {
        return currentWind;
    }

    public void setCurrentWind(Integer currentWind) {
        this.currentWind = currentWind;
    }

    public Integer getGustWind() {
        return gustWind;
    }

    public void setGustWind(Integer gustWind) {
        this.gustWind = gustWind;
    }
}
