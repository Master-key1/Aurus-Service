package com.auruspay.util;
public class TimeResult {

    private boolean withinOneHour;
    private String formattedHour;

    public TimeResult(boolean withinOneHour, String formattedHour) {
        this.withinOneHour = withinOneHour;
        this.formattedHour = formattedHour;
    }

    public boolean isWithinOneHour() {
        return withinOneHour;
    }

    public String getFormattedHour() {
        return formattedHour;
    }
}