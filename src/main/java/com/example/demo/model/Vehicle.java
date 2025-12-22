package com.example.demo.model;

import javafx.scene.image.Image;

public class Vehicle {
    private String id;
    private double latitude;
    private double longitude;
    private double vitesse; // km/h
    private String routeId;
    private Image image;
    private String type;

    public Vehicle(String id, double latitude, double longitude, double vitesse, String routeId, String imagePath, String type) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.vitesse = vitesse;
        this.routeId = routeId;
        this.type = type;
        
        // 🔥 CORRECTION : Gestion robuste du chargement d'images
        this.image = loadVehicleImage(imagePath, type);
    }

    private Image loadVehicleImage(String imagePath, String vehicleType) {
        try {
            System.out.println("🖼️ Tentative de chargement: " + imagePath);
            
            // Essayer le chemin fourni
            Image img = new Image(getClass().getResourceAsStream(imagePath));
            if (img != null && !img.isError()) {
                System.out.println("✅ Image chargée: " + imagePath);
                return img;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement " + imagePath + ": " + e.getMessage());
        }

        // 🔥 SOLUTION DE SECOURS : Créer une image colorée
        System.out.println("🎨 Création image par défaut pour: " + vehicleType);
        return createDefaultVehicleImage(vehicleType);
    }

    private Image createDefaultVehicleImage(String vehicleType) {
        // Créer une forme colorée selon le type de véhicule
        javafx.scene.shape.Rectangle shape = new javafx.scene.shape.Rectangle(40, 20);
        
        switch (vehicleType.toLowerCase()) {
            case "camion":
                shape.setFill(javafx.scene.paint.Color.RED);
                break;
            case "bus":
                shape.setFill(javafx.scene.paint.Color.BLUE);
                break;
            case "voiture":
            default:
                shape.setFill(javafx.scene.paint.Color.GREEN);
                break;
        }
        
        // Convertir la forme en image
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return shape.snapshot(params, null);
    }

    // Getters
    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getVitesse() { return vitesse; }
    public String getRouteId() { return routeId; }
    public Image getImage() { return image; }
    public String getType() { return type; }

    // Setters
    public void setPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setVitesse(double vitesse) {
        this.vitesse = vitesse;
    }
}