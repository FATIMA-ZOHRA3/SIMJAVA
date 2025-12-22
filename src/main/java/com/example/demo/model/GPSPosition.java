package com.example.demo.model;

import java.time.LocalDateTime;

public class GPSPosition {
    private double latitude;
    private double longitude;
    private String nom;
    private String description;
    private LocalDateTime dateSauvegarde;

    public GPSPosition(double latitude, double longitude, String nom, String description) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.nom = nom;
        this.description = description;
        this.dateSauvegarde = LocalDateTime.now();
    }

    // Getters
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public LocalDateTime getDateSauvegarde() { return dateSauvegarde; }
}