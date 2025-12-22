package com.example.demo.controller;

import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import javafx.animation.AnimationTimer;
import java.util.*;

public class SimulationManager {
    private Map<String, Route> routes;
    private AnimationTimer simulationTimer;
    private boolean enCours = false;

    public SimulationManager() {
        this.routes = new HashMap<>();
        initialiserRoutesTest();
    }

    private void initialiserRoutesTest() {
        // Route Casablanca - Rabat
        Route route1 = new Route("route1", "Casablanca → Rabat");
        route1.ajouterPoint(33.5731, -7.5898); // Casablanca
        route1.ajouterPoint(33.9692, -6.9272); // Rabat

        // Ajouter véhicules de test
        route1.ajouterVehicule(new Vehicle("v1", 33.5731, -7.5898, 80.0, "route1",
                "/images/car1.png", "Voiture"));
        route1.ajouterVehicule(new Vehicle("v2", 33.6000, -7.5500, 90.0, "route1",
                "/images/car2.png", "Camion"));
        route1.ajouterVehicule(new Vehicle("v3", 33.6500, -7.4500, 70.0, "route1",
                "/images/bus1.png", "Bus"));

        routes.put("route1", route1);

        // Route Casablanca - Marrakech
        Route route2 = new Route("route2", "Casablanca → Marrakech");
        route2.ajouterPoint(33.5731, -7.5898); // Casablanca
        route2.ajouterPoint(31.6295, -7.9811); // Marrakech

        route2.ajouterVehicule(new Vehicle("v4", 33.5731, -7.5898, 85.0, "route2",
                "/images/car3.png", "Voiture"));
        route2.ajouterVehicule(new Vehicle("v5", 32.5000, -7.8000, 75.0, "route2",
                "/images/truck1.png", "Camion"));

        routes.put("route2", route2);
    }

    public void demarrerSimulation(String routeId) {
        if (enCours) {
            arreterSimulation();
        }

        enCours = true;
        Route route = routes.get(routeId);

        if (route == null) return;

        simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                // Mettre à jour la position de chaque véhicule
                for (Vehicle vehicle : route.getVehicules()) {
                    deplacerVehicule(vehicle, route, elapsedSeconds);
                }
            }
        };

        simulationTimer.start();
        System.out.println("🚦 Simulation démarrée pour: " + route.getNom());
    }

    private void deplacerVehicule(Vehicle vehicle, Route route, double elapsedSeconds) {
        List<Route.Point> points = route.getPoints();
        if (points.size() < 2) return;

        // Simulation de mouvement simple (avance le long de la route)
        double vitesseMs = vehicle.getVitesse() / 3.6; // Conversion km/h -> m/s
        double distanceParcourue = vitesseMs * elapsedSeconds * 0.00001; // Facteur d'échelle

        // Calculer la nouvelle position (simplifié)
        double newLat = vehicle.getLatitude() + distanceParcourue;
        double newLng = vehicle.getLongitude() + distanceParcourue;

        // Vérifier les limites de la route
        if (newLat > points.get(points.size() - 1).getLatitude()) {
            newLat = points.get(0).getLatitude(); // Retour au début
            newLng = points.get(0).getLongitude();
        }

        vehicle.setPosition(newLat, newLng);
    }

    public void arreterSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        enCours = false;
        System.out.println("🛑 Simulation arrêtée");
    }

    public List<Route> getRoutesDisponibles() {
        return new ArrayList<>(routes.values());
    }

    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public boolean isEnCours() {
        return enCours;
    }
}