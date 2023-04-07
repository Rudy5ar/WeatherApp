package com.example.weatherapp;

public class ForecastItem {

    private String date;
    private String temperature;

    public ForecastItem(String date, String temperature) {
        this.date = date;
        this.temperature = temperature;
    }

    public String getDate() {
        return date;
    }

    public String getTemperature() {
        return temperature;
    }

}

