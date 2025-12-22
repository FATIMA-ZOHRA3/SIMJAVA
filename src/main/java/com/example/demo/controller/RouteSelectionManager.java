package com.example.demo.controller;

import com.example.demo.model.Route;
import javafx.application.Platform;
import java.util.*;

public class RouteSelectionManager {
    private List<Route> routes;
    private Map<String, Route> routeMap;
    private RouteSelectionListener listener;

    public interface RouteSelectionListener {
        void onRouteSelected(Route route);
    }

    public RouteSelectionManager() {
        this.routes = new ArrayList<>();
        this.routeMap = new HashMap<>();
        initialiserRoutes();
    }

    private void initialiserRoutes() {
        // Route Casablanca - Rabat
        Route route1 = new Route("route1", "Autoroute A1 - Casablanca → Rabat");
        route1.ajouterPoint(33.5731, -7.5898);
        route1.ajouterPoint(33.5928, -7.6186);
        route1.ajouterPoint(33.8300, -7.2000);
        route1.ajouterPoint(33.9692, -6.9272);
        
        // Route Casablanca - Marrakech
        Route route2 = new Route("route2", "Autoroute A7 - Casablanca → Marrakech");
        route2.ajouterPoint(33.5731, -7.5898);
        route2.ajouterPoint(33.4500, -7.6500);
        route2.ajouterPoint(32.8000, -7.9000);
        route2.ajouterPoint(31.6295, -7.9811);

        routes.add(route1);
        routes.add(route2);
        
        routeMap.put("route1", route1);
        routeMap.put("route2", route2);
        
        System.out.println("🛣️ RouteSelectionManager: " + routes.size() + " routes chargées");
    }

    public void setSelectionListener(RouteSelectionListener listener) {
        this.listener = listener;
    }

    // 🔥 NOUVELLE MÉTHODE: Détection de route par coordonnées de clic
    public void handleMapClick(double lat, double lng) {
        System.out.println("🖱️ Clic carte détecté: " + lat + ", " + lng);
        
        Route routeTrouvee = trouverRouteProche(lat, lng);
        
        if (routeTrouvee != null && listener != null) {
            System.out.println("🎯 Route sélectionnée: " + routeTrouvee.getNom());
            Platform.runLater(() -> listener.onRouteSelected(routeTrouvee));
        } else {
            System.out.println("❌ Aucune route trouvée à cette position");
        }
    }

    private Route trouverRouteProche(double latClic, double lngClic) {
        Route routePlusProche = null;
        double distanceMin = Double.MAX_VALUE;
        
        for (Route route : routes) {
            double distance = calculerDistanceRoute(route, latClic, lngClic);
            if (distance < distanceMin && distance < 0.02) { // Seuil de 0.02 degrés (~2km)
                distanceMin = distance;
                routePlusProche = route;
            }
        }
        
        return routePlusProche;
    }

    private double calculerDistanceRoute(Route route, double latClic, double lngClic) {
        double distanceMin = Double.MAX_VALUE;
        
        for (Route.Point point : route.getPoints()) {
            double distance = Math.sqrt(
                Math.pow(point.getLatitude() - latClic, 2) + 
                Math.pow(point.getLongitude() - lngClic, 2)
            );
            distanceMin = Math.min(distanceMin, distance);
        }
        
        return distanceMin;
    }

    public List<Route> getRoutes() {
        return new ArrayList<>(routes);
    }
}
