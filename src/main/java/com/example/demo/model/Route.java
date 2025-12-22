package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class Route {
    private String id;
    private String nom;
    private String type; // Champ ajouté
    private List<Point> points;
    private List<Vehicle> vehicules;
    private List<FeuCirculation> feuxCirculation;

    // Constructeur avec 3 paramètres (recommandé)
    public Route(String id, String nom, String type) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.points = new ArrayList<>();
        this.vehicules = new ArrayList<>();
        this.feuxCirculation = new ArrayList<>();
        System.out.println("🛣️ Route créée: " + nom + " (ID: " + id + ", Type: " + type + ")");
    }

    // Constructeur avec 2 paramètres (pour compatibilité)
    public Route(String id, String nom) {
        this(id, nom, "Non spécifié");
    }

    // Getters
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getType() { return type; }
    public List<Point> getPoints() { return points; }
    public List<Vehicle> getVehicules() { return vehicules; }
    public List<FeuCirculation> getFeuxCirculation() { return feuxCirculation; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setType(String type) { this.type = type; }

    // 🔥 CORRECTION : Renommer ajouterPoint en addPoint OU ajouter les deux méthodes
    public void addPoint(double latitude, double longitude) {
        this.points.add(new Point(latitude, longitude));
    }

    // 🔥 CONSERVER POUR COMPATIBILITÉ
    public void ajouterPoint(double latitude, double longitude) {
        addPoint(latitude, longitude);
    }

    public void ajouterVehicule(Vehicle vehicle) {
        vehicules.add(vehicle);
    }

    public void ajouterFeuCirculation(FeuCirculation feu) {
        if (feu == null) {
            System.err.println("❌ Tentative d'ajout d'un feu null");
            return;
        }
        this.feuxCirculation.add(feu);
        System.out.println("✅ Feu de circulation ajouté à " + nom + " : " + feu.getNom());
    }

    public void mettreAJourFeux() {
        for (FeuCirculation feu : feuxCirculation) {
            feu.mettreAJourEtat();
        }
    }

    public FeuCirculation trouverFeuProche(double latitude, double longitude, double distanceMax) {
        FeuCirculation feuProche = null;
        double distanceMin = Double.MAX_VALUE;

        for (FeuCirculation feu : feuxCirculation) {
            double distance = calculerDistance(latitude, longitude, feu.getLatitude(), feu.getLongitude());
            if (distance < distanceMax && distance < distanceMin) {
                distanceMin = distance;
                feuProche = feu;
            }
        }
        return feuProche;
    }

    private double calculerDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat2 - lat1;
        double dLng = lng2 - lng1;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    // Classe interne Point
    public static class Point {
        private double latitude;
        private double longitude;

        public Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}