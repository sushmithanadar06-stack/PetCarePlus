package com.example.petcareplus.models;

public class WeatherInfo {

    public double temperature;
    public double humidity;
    public boolean isRaining;
    public String condition;

    public WeatherInfo(double temperature, double humidity, boolean isRaining, String condition) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.isRaining = isRaining;
        this.condition = condition;
    }
}