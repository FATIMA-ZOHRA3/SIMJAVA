package com.example.demo.model;

import java.time.LocalDateTime;

public class Incident {
    private double latitude;
    private double longitude;
    private String type;
    private String description;
    private String severite;
    private LocalDateTime dateSignalement;

    public Incident(double latitude, double longitude, String type, String description, String severite) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.description = description;
        this.severite = severite;
        this.dateSignalement = LocalDateTime.now();
    }

    // Getters
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getSeverite() { return severite; }
    public LocalDateTime getDateSignalement() { return dateSignalement; }
}