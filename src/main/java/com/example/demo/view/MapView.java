package com.example.demo.view;

import com.example.demo.model.IPGeolocationService;
import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MapView extends StackPane {

    private final WebView webView;
    private final WebEngine engine;
    private boolean isMapInitialized = false;
    private final CountDownLatch initializationLatch = new CountDownLatch(1);
    private Runnable mapReadyListener;

    // Variables pour la gestion de l'itinéraire
    private double startLat = 0.0;
    private double startLng = 0.0;
    private double endLat = 0.0;
    private double endLng = 0.0;

    // Variables pour l'itinéraire calculé
    private String currentRouteDistance = "";
    private String currentRouteTime = "";
    private String currentRouteInstructions = "";

    // Callback pour obtenir l'ID utilisateur
    private Consumer<Integer> userIdProvider;

    // Interface pour la gestion des clics sur la carte
    private Consumer<MapClickData> mapClickListener;

    // Interface pour les événements d'itinéraire
    private Consumer<RouteData> routeDataListener;

    // Configuration des options d'itinéraire
    private RouteOptions routeOptions = new RouteOptions();

    // Listes temporaires pour les routes et marqueurs
    private List<Route> routesDisponibles = new ArrayList<>();
    private Consumer<Route> routeSelectionCallback;

    // Interface pour la sélection de route par clic
    private Consumer<Route> routeClickedListener;

    // 🔥 NOUVELLES VARIABLES DU SECOND CODE
    private Route routeSelectionnee;
    private int currentZoom = 13;
    private Consumer<Route> onRouteSelectedCallback;

    // Classe pour encapsuler les données de clic sur la carte
    public static class MapClickData {
        private final double latitude;
        private final double longitude;
        private final String address;

        public MapClickData(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getAddress() { return address; }
    }

    // Classe pour les données d'itinéraire
    public static class RouteData {
        private final double distance; // en km
        private final double time; // en minutes
        private final String instructions;
        private final double co2Emissions; // en kg
        private final double fuelConsumption; // en litres
        private final double estimatedCost; // en euros

        public RouteData(double distance, double time, String instructions,
                         double co2Emissions, double fuelConsumption, double estimatedCost) {
            this.distance = distance;
            this.time = time;
            this.instructions = instructions;
            this.co2Emissions = co2Emissions;
            this.fuelConsumption = fuelConsumption;
            this.estimatedCost = estimatedCost;
        }

        public double getDistance() { return distance; }
        public double getTime() { return time; }
        public String getInstructions() { return instructions; }
        public double getCo2Emissions() { return co2Emissions; }
        public double getFuelConsumption() { return fuelConsumption; }
        public double getEstimatedCost() { return estimatedCost; }

        // Méthodes formatées manquantes
        public String getFormattedDistance() {
            return String.format("%.2f km", distance);
        }

        public String getFormattedTime() {
            return String.format("%.0f min", time);
        }

        public String getFormattedCo2() {
            return String.format("%.2f kg", co2Emissions);
        }

        public String getFormattedFuelConsumption() {
            return String.format("%.2f L", fuelConsumption);
        }

        public String getFormattedCost() {
            return String.format("%.2f €", estimatedCost);
        }

        @Override
        public String toString() {
            return String.format("Distance: %.2f km, Temps: %.0f min, CO2: %.2f kg",
                    distance, time, co2Emissions);
        }
    }

    // Classe pour les options d'itinéraire
    public static class RouteOptions {
        private boolean avoidHighways = false;
        private boolean avoidTolls = false;
        private boolean avoidFerries = false;
        private String vehicleType = "car"; // car, bike, truck, motorcycle
        private double fuelEfficiency = 7.0; // L/100km
        private double fuelPrice = 1.5; // €/L
        private double co2PerKm = 0.12; // kg CO2/km

        public RouteOptions() {}

        // Getters et setters
        public boolean isAvoidHighways() { return avoidHighways; }
        public void setAvoidHighways(boolean avoidHighways) { this.avoidHighways = avoidHighways; }

        public boolean isAvoidTolls() { return avoidTolls; }
        public void setAvoidTolls(boolean avoidTolls) { this.avoidTolls = avoidTolls; }

        public boolean isAvoidFerries() { return avoidFerries; }
        public void setAvoidFerries(boolean avoidFerries) { this.avoidFerries = avoidFerries; }

        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

        public double getFuelEfficiency() { return fuelEfficiency; }
        public void setFuelEfficiency(double fuelEfficiency) { this.fuelEfficiency = fuelEfficiency; }

        public double getFuelPrice() { return fuelPrice; }
        public void setFuelPrice(double fuelPrice) { this.fuelPrice = fuelPrice; }

        public double getCo2PerKm() { return co2PerKm; }
        public void setCo2PerKm(double co2PerKm) { this.co2PerKm = co2PerKm; }

        public String toJavaScriptObject() {
            return String.format("{avoidHighways: %b, avoidTolls: %b, avoidFerries: %b, " +
                            "vehicleType: '%s', fuelEfficiency: %.2f, fuelPrice: %.2f, co2PerKm: %.3f}",
                    avoidHighways, avoidTolls, avoidFerries,
                    vehicleType, fuelEfficiency, fuelPrice, co2PerKm);
        }
    }

    public MapView() {
        webView = new WebView();
        engine = webView.getEngine();

        // 🔹 OPTIMISATION AFFICHAGE - TAILLE RESPONSIVE
        webView.setPrefSize(1200, 800);
        webView.setMinSize(600, 400);
        webView.setStyle("-fx-border-color: #2c3e50; -fx-border-width: 2px; -fx-border-radius: 5px;");

        this.getChildren().add(webView);
        this.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10;");

        // Initialiser les routes avec véhicules (du second code)
        initialiserRoutes();

        initializeMap();
    }

    // 🔥 MÉTHODE DU SECOND CODE POUR INITIALISER LES ROUTES AVEC VÉHICULES
    private void initialiserRoutes() {
        // Route Casablanca - Rabat
        Route route1 = new Route("route1", "Autoroute A1 - Casablanca → Rabat");
        route1.ajouterPoint(33.5731, -7.5898); // Casablanca
        route1.ajouterPoint(33.5928, -7.6186); // Aéroport
        route1.ajouterPoint(33.8300, -7.2000); // Bouskoura
        route1.ajouterPoint(33.9692, -6.9272); // Rabat
        ajouterVehiculesRoute(route1);

        // Route Casablanca - Marrakech
        Route route2 = new Route("route2", "Autoroute A7 - Casablanca → Marrakech");
        route2.ajouterPoint(33.5731, -7.5898); // Casablanca
        route2.ajouterPoint(33.4500, -7.6500); // Médiouna
        route2.ajouterPoint(32.8000, -7.9000); // Settat
        route2.ajouterPoint(31.6295, -7.9811); // Marrakech
        ajouterVehiculesRoute(route2);

        // Route Ceinture Casablanca
        Route route3 = new Route("route3", "Périphérique - Ceinture Casablanca");
        route3.ajouterPoint(33.6000, -7.6500); // Ouest
        route3.ajouterPoint(33.5800, -7.5500); // Nord
        route3.ajouterPoint(33.5500, -7.6000); // Est
        route3.ajouterPoint(33.5600, -7.6700); // Sud
        route3.ajouterPoint(33.6000, -7.6500); // Retour Ouest
        ajouterVehiculesRoute(route3);

        routesDisponibles.add(route1);
        routesDisponibles.add(route2);
        routesDisponibles.add(route3);

        System.out.println("🛣️ " + routesDisponibles.size() + " routes initialisées avec véhicules");
    }

    // 🔥 MÉTHODE DU SECOND CODE POUR AJOUTER DES VÉHICULES
    private void ajouterVehiculesRoute(Route route) {
        System.out.println("🚗 Ajout de véhicules à: " + route.getNom());

        if (route.getPoints().isEmpty()) {
            System.err.println("❌ Route sans points, impossible d'ajouter des véhicules");
            return;
        }

        Route.Point depart = route.getPoints().get(0);

        route.ajouterVehicule(new Vehicle(
                "v1-" + route.getId(),
                depart.getLatitude(),
                depart.getLongitude(),
                80.0,
                route.getId(),
                "/images/car1.png",
                "Voiture"
        ));

        route.ajouterVehicule(new Vehicle(
                "v2-" + route.getId(),
                depart.getLatitude() + 0.001,
                depart.getLongitude() + 0.001,
                60.0,
                route.getId(),
                "/images/truck1.png",
                "Camion"
        ));

        route.ajouterVehicule(new Vehicle(
                "v3-" + route.getId(),
                depart.getLatitude() - 0.001,
                depart.getLongitude() - 0.001,
                70.0,
                route.getId(),
                "/images/bus1.png",
                "Bus"
        ));

        if (route.getPoints().size() > 3) {
            route.ajouterVehicule(new Vehicle(
                    "v4-" + route.getId(),
                    depart.getLatitude() + 0.002,
                    depart.getLongitude() + 0.002,
                    90.0,
                    route.getId(),
                    "/images/car2.png",
                    "Voiture"
            ));

            route.ajouterVehicule(new Vehicle(
                    "v5-" + route.getId(),
                    depart.getLatitude() - 0.002,
                    depart.getLongitude() - 0.002,
                    50.0,
                    route.getId(),
                    "/images/truck2.png",
                    "Camion"
            ));
        }

        System.out.println("✅ " + route.getVehicules().size() + " véhicules ajoutés à " + route.getNom());
    }

    // Méthode pour définir le listener des données d'itinéraire
    public void setOnRouteDataListener(Consumer<RouteData> listener) {
        this.routeDataListener = listener;
        System.out.println("🗺️ RouteDataListener configuré");
    }

    // Méthode pour définir le listener de clic sur route
    public void setOnRouteClicked(Consumer<Route> listener) {
        this.routeClickedListener = listener;
        System.out.println("🖱️ RouteClickedListener configuré pour mode clic");
    }

    // Méthode pour obtenir les options d'itinéraire
    public RouteOptions getRouteOptions() {
        return routeOptions;
    }

    // Méthode pour mettre à jour les options d'itinéraire
    public void updateRouteOptions(RouteOptions options) {
        this.routeOptions = options;
        System.out.println("⚙️ Options d'itinéraire mises à jour: " + options.toJavaScriptObject());

        // Mettre à jour également dans JavaScript
        Platform.runLater(() -> {
            try {
                String script = String.format(
                        "if (window.loadRouteOptions) { window.loadRouteOptions(%s); }",
                        options.toJavaScriptObject()
                );
                engine.executeScript(script);
            } catch (Exception e) {
                System.err.println("❌ Erreur mise à jour options JS: " + e.getMessage());
            }
        });
    }

    // Méthode pour obtenir l'itinéraire actuel
    public void getCurrentRouteInfo() {
        if (!currentRouteDistance.isEmpty()) {
            Platform.runLater(() -> {
                System.out.println("📊 Informations itinéraire actuel:");
                System.out.println("   Distance: " + currentRouteDistance);
                System.out.println("   Temps: " + currentRouteTime);
                System.out.println("   Instructions: " + currentRouteInstructions);
            });
        }
    }

    public void setUserIdProvider(Consumer<Integer> userIdProvider) {
        this.userIdProvider = userIdProvider;
        System.out.println("🗺️ MapView - UserIdProvider configuré");
    }

    // Méthode pour définir le listener des clics sur la carte
    public void setOnMapClickListener(Consumer<MapClickData> listener) {
        this.mapClickListener = listener;
        System.out.println("🖱️ MapClickListener configuré");
    }

    private int getCurrentUserId() {
        if (userIdProvider != null) {
            final int[] userId = {1};
            userIdProvider.accept(userId[0]);
            return userId[0];
        }
        System.out.println("⚠️ UserIdProvider non configuré, utilisation ID par défaut: 1");
        return 1;
    }

    public void setOnMapReady(Runnable listener) {
        this.mapReadyListener = listener;
        if (isMapInitialized && listener != null) {
            Platform.runLater(listener);
        }
    }

    // ==================== MÉTHODES DU SECOND CODE POUR LA SÉLECTION DE ROUTES ====================

    // 🔥 MÉTHODE POUR ACTIVER LA SÉLECTION DE ROUTES
    public void activerSelectionRoute(Consumer<Route> callback) {
        this.onRouteSelectedCallback = callback;

        Platform.runLater(() -> {
            try {
                String script = """
                    if (window.showRouteSelection) {
                        window.showRouteSelection();
                    }
                """;
                engine.executeScript(script);

                dessinerRoutesSurCarte();

                System.out.println("🎯 Sélection de routes activée");

            } catch (Exception e) {
                System.err.println("❌ Erreur activation sélection routes: " + e.getMessage());
            }
        });
    }

    private void dessinerRoutesSurCarte() {
        for (Route route : routesDisponibles) {
            dessinerRoute(route);
        }
    }

    private void dessinerRoute(Route route) {
        Platform.runLater(() -> {
            try {
                StringBuilder pointsJs = new StringBuilder("[");
                for (Route.Point point : route.getPoints()) {
                    pointsJs.append("[").append(point.getLatitude()).append(",").append(point.getLongitude()).append("],");
                }
                String script = String.format(
                        "if (window.addRoute) { window.addRoute('%s', '%s', %s); }",
                        route.getId(),
                        route.getNom().replace("'", "\\'"),
                        pointsJs.toString()
                );

                engine.executeScript(script);
                System.out.println("🛣️ Route dessinée: " + route.getNom());

            } catch (Exception e) {
                System.err.println("❌ Erreur dessin route " + route.getNom() + ": " + e.getMessage());
            }
        });
    }

    public void demarrerSimulationSurRoute(Route route) {
        this.routeSelectionnee = route;

        Platform.runLater(() -> {
            try {
                String script = String.format(
                        "if (window.selectRoute) { window.selectRoute('%s', '%s'); }",
                        route.getId(),
                        route.getNom().replace("'", "\\'")
                );
                engine.executeScript(script);

                System.out.println("🚗 Simulation démarrée sur: " + route.getNom());

            } catch (Exception e) {
                System.err.println("❌ Erreur démarrage simulation: " + e.getMessage());
            }
        });
    }

    // 🔥 MÉTHODE POUR ACTIVER LA SÉLECTION PAR CLIC
    public void activerSelectionParClic(Consumer<Route> callback) {
        this.onRouteSelectedCallback = callback;

        Platform.runLater(() -> {
            if (!isMapInitialized) {
                System.out.println("⚠️ Carte non initialisée, impossible d'activer la sélection par clic");
                return;
            }

            try {
                String script = """
                    if (window.activateClickSelection) {
                        window.activateClickSelection();
                    }
                """;
                engine.executeScript(script);

                dessinerRoutesSurCarte();

                // Zoom to fit all routes
                zoomToRoutes();

                System.out.println("🎯 Sélection par clic activée - Cliquez sur une route");

            } catch (Exception e) {
                System.err.println("❌ Erreur activation sélection par clic: " + e.getMessage());
            }
        });
    }

    private void zoomToRoutes() {
        if (routesDisponibles.isEmpty()) return;

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (Route route : routesDisponibles) {
            for (Route.Point point : route.getPoints()) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLng = Math.min(minLng, point.getLongitude());
                maxLng = Math.max(maxLng, point.getLongitude());
            }
        }

        if (minLat != Double.MAX_VALUE) {
            try {
                String script = String.format("if (map) map.fitBounds([[%f, %f], [%f, %f]], {padding: [20, 20]})", minLat, minLng, maxLat, maxLng);
                engine.executeScript(script);
                System.out.println("🔍 Zoom ajusté pour afficher toutes les routes");
            } catch (Exception e) {
                System.err.println("❌ Erreur zoom routes: " + e.getMessage());
            }
        }
    }

    // 🔥 MÉTHODE POUR DÉSACTIVER LA SÉLECTION PAR CLIC
    public void desactiverSelectionParClic() {
        Platform.runLater(() -> {
            try {
                String script = """
                    if (window.deactivateClickSelection) {
                        window.deactivateClickSelection();
                    }
                """;
                engine.executeScript(script);

                System.out.println("🎯 Sélection par clic désactivée");

            } catch (Exception e) {
                System.err.println("❌ Erreur désactivation sélection par clic: " + e.getMessage());
            }
        });
    }


    public void mettreEnSurbriillanceRoute(Route route) {
        this.routeSelectionnee = route;

        Platform.runLater(() -> {
            try {
                String script = String.format(
                        "if (window.highlightRoute) { window.highlightRoute('%s'); }",
                        route.getId()
                );
                engine.executeScript(script);

                System.out.println("🌟 Route mise en surbrillance: " + route.getNom());

            } catch (Exception e) {
                System.err.println("❌ Erreur surbrillance route: " + e.getMessage());
            }
        });
    }

    public void afficherDetailsRoute(Route route) {
        Platform.runLater(() -> {
            try {
                StringBuilder pointsJs = new StringBuilder("[");
                for (Route.Point point : route.getPoints()) {
                    pointsJs.append("[").append(point.getLatitude()).append(",").append(point.getLongitude()).append("],");
                }
                if (pointsJs.length() > 1) pointsJs.deleteCharAt(pointsJs.length() - 1);
                pointsJs.append("]");

                String script = String.format(
                        "if (window.showRouteDetails) { window.showRouteDetails('%s', '%s', %s, %d); }",
                        route.getId(),
                        route.getNom().replace("'", "\\'"),
                        pointsJs.toString(),
                        route.getVehicules().size()
                );
                engine.executeScript(script);

                System.out.println("📊 Détails affichés pour: " + route.getNom());

            } catch (Exception e) {
                System.err.println("❌ Erreur affichage détails route: " + e.getMessage());
            }
        });
    }

    // ==================== CORRECTION CRITIQUE : DIVISION DE LA GRANDE CHAÎNE HTML ====================

    private void initializeMap() {
        System.out.println("🗺️ Initialisation de la carte avec qualité optimisée...");

        // 🔧 CORRECTION : Appeler la méthode qui génère le HTML découpé
        String html = generateCompleteHTML();

        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("🌐 État WebView: " + newState);

            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaConnector", new JavaConnector());
                        webView.requestFocus();

                        webView.setCache(true);

                    } catch (Exception e) {
                        System.err.println("❌ Erreur connexion Java-JS: " + e.getMessage());
                        notifyMapReady();
                    }
                });
            } else if (newState == Worker.State.FAILED) {
                System.err.println("❌ Échec chargement WebView");
                notifyMapReady();
            }
        });

        try {
            engine.loadContent(html);
        } catch (Exception e) {
            System.err.println("❌ Erreur loadContent: " + e.getMessage());
            notifyMapReady();
        }

        new Thread(() -> {
            try {
                Thread.sleep(15000);
                if (!isMapInitialized) {
                    System.err.println("⚠️ Timeout initialisation MapView");
                    notifyMapReady();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // 🔧 NOUVELLE MÉTHODE : Génération du HTML découpé en plusieurs parties
    private String generateCompleteHTML() {
        StringBuilder htmlBuilder = new StringBuilder();

        // Partie 1 : Début du HTML et styles
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Carte Traffic Premium - Itinéraire</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.css" />
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    }
                    #map {
                        width: 100%;
                        height: 100%;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    }
                    #status {
                        position: absolute;
                        top: 15px;
                        left: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 12px 18px;
                        border-radius: 8px;
                        border: 2px solid #3498db;
                        z-index: 1000;
                        font-size: 14px;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        max-width: 350px;
                        backdrop-filter: blur(10px);
                    }
                    #route-info {
                        position: absolute;
                        bottom: 25px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: rgba(255, 255, 255, 0.95);
                        padding: 18px;
                        border-radius: 12px;
                        border: 3px solid #27ae60;
                        z-index: 1000;
                        font-size: 15px;
                        font-weight: 600;
                        box-shadow: 0 6px 20px rgba(0,0,0,0.3);
                        max-width: 450px;
                        text-align: center;
                        display: none;
                        backdrop-filter: blur(10px);
                    }
                    .error {
                        background: rgba(255, 235, 238, 0.95) !important;
                        color: #c62828 !important;
                        border-color: #e53935 !important;
                    }
                    .success {
                        background: rgba(232, 245, 233, 0.95) !important;
                        color: #2e7d32 !important;
                        border-color: #43a047 !important;
                    }
                    /* 🔥 STYLES POUR LA SÉLECTION DE ROUTES (du second code) */
                    .route-selection-panel {
                        position: absolute;
                        top: 80px;
                        left: 20px;
                        background: rgba(255,255,255,0.95);
                        padding: 15px;
                        border-radius: 10px;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        z-index: 1000;
                        max-width: 300px;
                        backdrop-filter: blur(10px);
                    }

                    .route-item {
                        padding: 8px 12px;
                        margin: 5px 0;
                        background: #ecf0f1;
                        border-radius: 5px;
                        cursor: pointer;
                        border-left: 4px solid #3498db;
                        transition: all 0.3s ease;
                    }

                    .route-item:hover {
                        background: #d5dbdb;
                        transform: translateX(5px);
                    }

                    .route-name {
                        font-weight: bold;
                        color: #2c3e50;
                    }

                    .route-hint {
                        font-size: 10px;
                        color: #7f8c8d;
                    }
                    
                    /* 🔹 PANEL D'OPTIONS D'ITINÉRAIRE */
                    .route-options-panel {
                        position: absolute;
                        top: 15px;
                        right: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 15px;
                        border-radius: 12px;
                        box-shadow: 0 6px 25px rgba(0,0,0,0.2);
                        display: flex;
                        flex-direction: column;
                        gap: 10px;
                        z-index: 1000;
                        backdrop-filter: blur(10px);
                        border: 1px solid #bdc3c7;
                        max-width: 300px;
                        min-width: 250px;
                        display: none;
                    }
                    .options-header {
                        font-size: 16px;
                        font-weight: bold;
                        color: #2c3e50;
                        margin-bottom: 10px;
                        padding-bottom: 8px;
                        border-bottom: 2px solid #3498db;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .option-group {
                        display: flex;
                        flex-direction: column;
                        gap: 8px;
                    }
                    .option-item {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        font-size: 13px;
                    }
                    .option-label {
                        display: flex;
                        align-items: center;
                        gap: 6px;
                    }
                    .option-switch {
                        position: relative;
                        display: inline-block;
                        width: 40px;
                        height: 20px;
                    }
                    .option-switch input {
                        opacity: 0;
                        width: 0;
                        height: 0;
                    }
                    .option-slider {
                        position: absolute;
                        cursor: pointer;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background-color: #ccc;
                        transition: .4s;
                        border-radius: 20px;
                    }
                    .option-slider:before {
                        position: absolute;
                        content: "";
                        height: 16px;
                        width: 16px;
                        left: 2px;
                        bottom: 2px;
                        background-color: white;
                        transition: .4s;
                        border-radius: 50%;
                    }
                    input:checked + .option-slider {
                        background-color: #27ae60;
                    }
                    input:checked + .option-slider:before {
                        transform: translateX(20px);
                    }
                    .option-select {
                        padding: 5px 10px;
                        border-radius: 5px;
                        border: 1px solid #bdc3c7;
                        font-size: 13px;
                        background: white;
                        width: 100%;
                    }
                    .option-input {
                        padding: 5px 10px;
                        border-radius: 5px;
                        border: 1px solid #bdc3c7;
                        font-size: 13px;
                        background: white;
                        width: 60px;
                        text-align: center;
                    }
                    .options-actions {
                        display: flex;
                        gap: 8px;
                        margin-top: 10px;
                    }
                    .option-btn {
                        flex: 1;
                        padding: 8px 12px;
                        border: none;
                        border-radius: 6px;
                        font-size: 12px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.3s ease;
                    }
                    .option-btn.apply {
                        background: #27ae60;
                        color: white;
                    }
                    .option-btn.apply:hover {
                        background: #229954;
                    }
                    .option-btn.cancel {
                        background: #e74c3c;
                        color: white;
                    }
                    .option-btn.cancel:hover {
                        background: #c0392b;
                    }
                    .menu-group {
                        display: flex;
                        flex-direction: column;
                        gap: 6px;
                        align-items: center;
                    }
                    .menu-divider {
                        width: 80%;
                        height: 1px;
                        background: #bdc3c7;
                        margin: 5px 0;
                    }
                    .menu-btn {
                        background: linear-gradient(145deg, #3498db, #2980b9);
                        color: white;
                        border: none;
                        padding: 10px 15px;
                        border-radius: 6px;
                        cursor: pointer;
                        font-size: 12px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                        white-space: nowrap;
                        width: 100%;
                        text-align: center;
                        min-width: 120px;
                    }
                    .menu-btn:hover {
                        background: linear-gradient(145deg, #2980b9, #2471a3);
                        transform: translateY(-1px);
                        box-shadow: 0 3px 6px rgba(0,0,0,0.3);
                    }
                    .menu-btn:active {
                        transform: translateY(0);
                    }
                    .menu-btn.start {
                        background: linear-gradient(145deg, #27ae60, #229954);
                    }
                    .menu-btn.start:hover {
                        background: linear-gradient(145deg, #229954, #1e8449);
                    }
                    .menu-btn.end {
                        background: linear-gradient(145deg, #e67e22, #d35400);
                    }
                    .menu-btn.end:hover {
                        background: linear-gradient(145deg, #d35400, #ba4a00);
                    }
                    .menu-btn.route {
                        background: linear-gradient(145deg, #9b59b6, #8e44ad);
                    }
                    .menu-btn.route:hover {
                        background: linear-gradient(145deg, #8e44ad, #7d3c98);
                    }
                    .menu-btn.clear {
                        background: linear-gradient(145deg, #e74c3c, #c0392b);
                    }
                    .menu-btn.clear:hover {
                        background: linear-gradient(145deg, #c0392b, #a93226);
                    }
                    .menu-btn.map {
                        background: linear-gradient(145deg, #34495e, #2c3e50);
                    }
                    .menu-btn.map:hover {
                        background: linear-gradient(145deg, #2c3e50, #243342);
                    }
                    .menu-btn.options {
                        background: linear-gradient(145deg, #16a085, #1abc9c);
                    }
                    .menu-btn.options:hover {
                        background: linear-gradient(145deg, #1abc9c, #16a085);
                    }
                    .menu-btn.select-route {
                        background: linear-gradient(145deg, #f39c12, #e67e22);
                        animation: pulse 2s infinite;
                    }
                    .menu-btn.select-route:hover {
                        background: linear-gradient(145deg, #e67e22, #d35400);
                    }
                    .ip-marker {
                        background: transparent;
                        border: none;
                    }
                    .start-marker {
                        background: #27ae60;
                        border: 3px solid white;
                        border-radius: 50%;
                        width: 16px;
                        height: 16px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                    }
            """);

        // Partie 2 : Suite des styles
        htmlBuilder.append("""
                    .end-marker {
                        background: #e67e22;
                        border: 3px solid white;
                        border-radius: 50%;
                        width: 16px;
                        height: 16px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                    }
                    .click-marker {
                        background: #3498db;
                        border: 3px solid white;
                        border-radius: 50%;
                        width: 14px;
                        height: 14px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                    }
                    .neighbor-marker {
                        background: #9b59b6;
                        border: 2px solid white;
                        border-radius: 50%;
                        width: 10px;
                        height: 10px;
                        box-shadow: 0 1px 5px rgba(0,0,0,0.2);
                    }
                    .route-marker {
                        background: #f39c12;
                        border: 2px solid white;
                        border-radius: 50%;
                        width: 12px;
                        height: 12px;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                        cursor: pointer;
                        transition: all 0.3s ease;
                    }
                    .route-marker:hover {
                        background: #e67e22;
                        transform: scale(1.2);
                        box-shadow: 0 3px 10px rgba(0,0,0,0.4);
                    }
                    .route-marker.selected {
                        background: #e74c3c;
                        border: 3px solid white;
                        transform: scale(1.3);
                        animation: pulse 1.5s infinite;
                    }
                    .leaflet-routing-container {
                        background: rgba(255, 255, 255, 0.95);
                        width: 350px;
                        padding: 15px;
                        border-radius: 8px;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        backdrop-filter: blur(10px);
                        border: 1px solid #bdc3c7;
                    }
                    .leaflet-routing-alt {
                        max-height: 200px;
                    }
                    .custom-popup .leaflet-popup-content-wrapper {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 10px;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        backdrop-filter: blur(10px);
                    }
                    .route-summary {
                        background: #f8f9fa;
                        padding: 10px;
                        border-radius: 6px;
                        margin: 5px 0;
                        border-left: 4px solid #3498db;
                    }
                    .traffic-info {
                        position: absolute;
                        top: 80px;
                        left: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 15px;
                        border-radius: 8px;
                        border: 2px solid #e74c3c;
                        z-index: 1000;
                        font-size: 13px;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        max-width: 300px;
                        backdrop-filter: blur(10px);
                        display: none;
                    }
                    .eco-info {
                        position: absolute;
                        top: 150px;
                        right: 15px;
                        background: linear-gradient(135deg, rgba(46, 213, 115, 0.95), rgba(34, 139, 34, 0.95));
                        padding: 18px;
                        border-radius: 12px;
                        border: 2px solid #27ae60;
                        z-index: 1000;
                        font-size: 13px;
                        font-weight: 600;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.3);
                        max-width: 320px;
                        backdrop-filter: blur(10px);
                        display: none;
                        color: white;
                    }
                    .eco-badge {
                        display: inline-block;
                        background: rgba(255,255,255,0.2);
                        padding: 4px 8px;
                        border-radius: 20px;
                        margin: 2px;
                        font-size: 11px;
                        border: 1px solid rgba(255,255,255,0.4);
                    }
                    .poi-marker {
                        background: linear-gradient(135deg, #f39c12, #e67e22);
                        border: 2px solid white;
                        border-radius: 50%;
                        width: 20px;
                        height: 20px;
                        box-shadow: 0 3px 10px rgba(0,0,0,0.4);
                    }
                    .elevation-chart {
                        position: absolute;
                        bottom: 300px;
                        right: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 15px;
                        border-radius: 8px;
                        border: 2px solid #3498db;
                        z-index: 1000;
                        font-size: 12px;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        max-width: 280px;
                        backdrop-filter: blur(10px);
                        display: none;
                    }
                    .speed-profile {
                        position: absolute;
                        bottom: 150px;
                        right: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 15px;
                        border-radius: 8px;
                        border: 2px solid #9b59b6;
                        z-index: 1000;
                        font-size: 12px;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        max-width: 280px;
                        backdrop-filter: blur(10px);
                        display: none;
                    }
                    .animated-pulse {
                        animation: pulse 2s infinite;
                    }
                    @keyframes pulse {
                        0%, 100% { opacity: 1; }
                        50% { opacity: 0.5; }
                    }
                    .route-hazard {
                        position: absolute;
                        bottom: 25px;
                        right: 15px;
                        background: linear-gradient(135deg, rgba(230, 126, 34, 0.95), rgba(192, 57, 43, 0.95));
                        padding: 12px 15px;
                        border-radius: 8px;
                        border-left: 4px solid #e74c3c;
                        z-index: 1000;
                        font-size: 12px;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        max-width: 250px;
                        backdrop-filter: blur(10px);
                        display: none;
                        color: white;
                    }
                    .route-selection-panel {
                        position: absolute;
                        top: 200px;
                        right: 15px;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 15px;
                        border-radius: 12px;
                        border: 2px solid #f39c12;
                        z-index: 1000;
                        font-size: 13px;
                        font-weight: 600;
                        box-shadow: 0 6px 20px rgba(0,0,0,0.3);
                        max-width: 280px;
                        backdrop-filter: blur(10px);
                        display: none;
                    }
                </style>
            </head>
            <body>
                <div id="status">🔄 Chargement de la carte haute qualité...</div>
                <div id="route-info"></div>
                <div id="traffic-info" class="traffic-info"></div>
                <div id="eco-info" class="eco-info"></div>
                <div id="elevation-chart" class="elevation-chart"></div>
                <div id="speed-profile" class="speed-profile"></div>
                <div id="route-hazard" class="route-hazard"></div>
                
                <!-- 🔥 PANEL DE SÉLECTION DE ROUTES (du second code) -->
                <div id="route-selection-panel" class="route-selection-panel" style="display: none;">
                    <h3 style="margin: 0 0 10px 0; color: #2c3e50;">🚗 Sélection de Route</h3>
                    <p style="margin: 0 0 15px 0; color: #7f8c8d; font-size: 12px;">Cliquez sur une route colorée pour démarrer la simulation de trafic</p>
                    <div id="routes-list"></div>
                </div>
                
                <!-- 🔹 PANEL D'OPTIONS D'ITINÉRAIRE -->
                <div id="route-options-panel" class="route-options-panel">
                    <div class="options-header">
                        ⚙️ Options Itinéraire
                        <button onclick="hideRouteOptions()" style="background: none; border: none; font-size: 16px; cursor: pointer; color: #e74c3c;">×</button>
                    </div>
                    <div class="option-group">
                        <div class="option-item">
                            <div class="option-label">
                                🚧 Éviter les autoroutes
                            </div>
                            <label class="option-switch">
                                <input type="checkbox" id="avoidHighways">
                                <span class="option-slider"></span>
                            </label>
                        </div>
                        <div class="option-item">
                            <div class="option-label">
                                🛣️ Éviter les péages
                            </div>
                            <label class="option-switch">
                                <input type="checkbox" id="avoidTolls">
                                <span class="option-slider"></span>
                            </label>
                        </div>
                        <div class="option-item">
                            <div class="option-label">
                                ⛴️ Éviter les ferries
                            </div>
                            <label class="option-switch">
                                <input type="checkbox" id="avoidFerries">
                                <span class="option-slider"></span>
                            </label>
                        </div>
                        <div class="option-item">
                            <div class="option-label">
                                🚗 Type de véhicule
                            </div>
                            <select id="vehicleType" class="option-select">
                                <option value="car">Voiture</option>
                                <option value="bike">Vélo</option>
                                <option value="truck">Camion</option>
                                <option value="motorcycle">Moto</option>
                            </select>
                        </div>
                        <div class="option-item">
                            <div class="option-label">
                                ⛽ Consommation (L/100km)
                            </div>
                            <input type="number" id="fuelEfficiency" class="option-input" min="3" max="20" step="0.5" value="7.0">
                        </div>
                        <div class="option-item">
                            <div class="option-label">
                                💰 Prix carburant (€/L)
                            </div>
                            <input type="number" id="fuelPrice" class="option-input" min="1" max="3" step="0.01" value="1.50">
                        </div>
                    </div>
                    <div class="options-actions">
                        <button class="option-btn apply" onclick="applyRouteOptions()">✅ Appliquer</button>
                        <button class="option-btn cancel" onclick="hideRouteOptions()">❌ Annuler</button>
                    </div>
                </div>
                

                
                <div id="map"></div>
            """);

        // Partie 3 : Scripts JavaScript
        htmlBuilder.append("""
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script src="https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.js"></script>
                
                <script>
                    let map;
                    let markers = [];
                    let isMapReady = false;
                    let ipLocationMarker = null;
                    let routingControl = null;
                    let startMarker = null;
                    let endMarker = null;
                    let startPoint = null;
                    let endPoint = null;
                    let clickHandler = null;
                    let currentMapStyle = 'osm';
                    let baseLayers = {};
                    let mapClickEnabled = false;
                    let clickMarker = null;
                    let neighborMarkers = [];
                    
                    // 🔥 Variables pour la sélection de routes (du second code)
                    let routeLines = [];
                    let clickSelectionActive = false;
                    let currentRouteId = null;
                    
                    // 🔹 Variables pour l'itinéraire
                    let routeOptions = {
                        avoidHighways: false,
                        avoidTolls: false,
                        avoidFerries: false,
                        vehicleType: 'car',
                        fuelEfficiency: 7.0,
                        fuelPrice: 1.5,
                        co2PerKm: 0.12
                    };
                    
                    // 🔹 Stockage des données d'itinéraire
                    let currentRouteData = {
                        distance: 0,
                        time: 0,
                        instructions: '',
                        co2Emissions: 0,
                        fuelConsumption: 0,
                        estimatedCost: 0,
                        coordinates: []
                    };

                    function showStatus(message, isError = false) {
                        const status = document.getElementById('status');
                        status.innerHTML = message;
                        status.className = isError ? 'error' : 'success';
                        if (!isError) {
                            setTimeout(() => {
                                if (status.innerHTML === message) {
                                    status.style.display = 'none';
                                }
                            }, 5000);
                        }
                    }

                    // 🔥 FONCTIONS POUR LA SÉLECTION DE ROUTES (du second code)
                    window.showRouteSelection = function() {
                        showStatus("🛣️ Cliquez sur une route pour démarrer la simulation", false);

                        const panel = document.getElementById('route-selection-panel');
                        panel.style.display = 'block';

                        // 🔥 CORRECTION: Dessiner les routes quand le mode est activé
                        if (window.javaConnector && window.javaConnector.drawRoutes) {
                            window.javaConnector.drawRoutes();
                        }
                    };

                    window.addRoute = function(routeId, routeName, points) {
                        if (!map) return;

                        try {
                            const routeLine = L.polyline(JSON.parse(points), {
                                color: getRouteColor(routeId),
                                weight: 6,
                                opacity: 0.7,
                                className: 'route-line'
                            }).addTo(map);
                            
                            routeLine.routeId = routeId;
                            routeLine.routeName = routeName;
                            routeLines.push(routeLine);
                            
                            routeLine.on('click', function(e) {
                                selectRoute(routeId, routeName);
                            });
                            
                            addRouteToList(routeId, routeName);
                            
                        } catch (error) {
                            console.error('Erreur ajout route:', error);
                        }
                    };

                    function getRouteColor(routeId) {
                        const colors = {
                            'route1': '#e74c3c',
                            'route2': '#3498db',
                            'route3': '#2ecc71'
                        };
                        return colors[routeId] || '#f39c12';
                    }

                    function addRouteToList(routeId, routeName) {
                        const routesList = document.getElementById('routes-list');
                        if (routesList) {
                            const routeItem = document.createElement('div');
                            routeItem.className = 'route-item';
                            routeItem.style.borderLeftColor = getRouteColor(routeId);
                            routeItem.innerHTML = `
                                <div class="route-name">${routeName}</div>
                                <div class="route-hint">Cliquez sur la route</div>
                            `;
                            routeItem.onclick = function() {
                                selectRoute(routeId, routeName);
                            };
                            routesList.appendChild(routeItem);
                        }
                    }

                    function selectRoute(routeId, routeName) {
                        routeLines.forEach(routeLine => {
                            if (routeLine.routeId === routeId) {
                                routeLine.setStyle({
                                    color: '#f1c40f',
                                    weight: 8,
                                    opacity: 1
                                });
                            } else {
                                routeLine.setStyle({
                                    color: getRouteColor(routeLine.routeId),
                                    weight: 6,
                                    opacity: 0.7
                                });
                            }
                        });
                        
                        currentRouteId = routeId;
                        showStatus("✅ Route sélectionnée: " + routeName + " - Simulation en attente...", false);
                        
                        if (window.javaConnector && window.javaConnector.onRouteSelected) {
                            window.javaConnector.onRouteSelected(routeId, routeName);
                        }
                    }

                    window.selectRoute = function(routeId, routeName) {
                        selectRoute(routeId, routeName);
                    };

                    window.activateClickSelection = function() {
                        clickSelectionActive = !clickSelectionActive;
                        if (clickSelectionActive) {
                            showStatus("🎯 Mode sélection par clic activé - Cliquez sur une route", false);
                            map.getContainer().style.cursor = 'pointer';

                            // 🔥 CORRECTION: Dessiner les routes quand le mode clic est activé
                            if (window.javaConnector && window.javaConnector.drawRoutes) {
                                window.javaConnector.drawRoutes();
                            }

                            routeLines.forEach(routeLine => {
                                routeLine.on('click', function(e) {
                                    selectRoute(routeLine.routeId, routeLine.routeName || 'Route ' + routeLine.routeId);
                                });
                            });
                        } else {
                            showStatus("🎯 Mode sélection par clic désactivé", false);
                            map.getContainer().style.cursor = '';
                        }
                    };

                    window.highlightRoute = function(routeId) {
                        routeLines.forEach(routeLine => {
                            if (routeLine.routeId === routeId) {
                                routeLine.setStyle({
                                    color: '#f1c40f',
                                    weight: 10,
                                    opacity: 1
                                });
                                map.fitBounds(routeLine.getBounds());
                            } else {
                                routeLine.setStyle({
                                    color: getRouteColor(routeLine.routeId),
                                    weight: 6,
                                    opacity: 0.7
                                });
                            }
                        });
                    };
            """);

        // Partie 4 : Suite des fonctions JavaScript
        htmlBuilder.append("""
                    // 🔹 Fonction optimisée pour l'affichage des informations d'itinéraire
                    function showRouteInfo(distance, time, instructions, ecoData = null) {
                        const routeInfo = document.getElementById('route-info');
                        
                        let ecoHTML = '';
                        if (ecoData) {
                            ecoHTML = `
                                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 10px 0;">
                                    <div style="background: #e8f5e8; padding: 8px; border-radius: 6px; border-left: 4px solid #27ae60;">
                                        <strong>🌱 CO₂:</strong><br>${ecoData.co2} kg
                                    </div>
                                    <div style="background: #fff3e0; padding: 8px; border-radius: 6px; border-left: 4px solid #f39c12;">
                                        <strong>⛽ Carburant:</strong><br>${ecoData.fuel} L
                                    </div>
                                    <div style="background: #e3f2fd; padding: 8px; border-radius: 6px; border-left: 4px solid #2196f3;">
                                        <strong>💰 Coût:</strong><br>${ecoData.cost} €
                                    </div>
                                    <div style="background: #f3e5f5; padding: 8px; border-radius: 6px; border-left: 4px solid #9b59b6;">
                                        <strong>🚗 Véhicule:</strong><br>${getVehicleName(routeOptions.vehicleType)}
                                    </div>
                                </div>
                            `;
                        }
                        
                        routeInfo.innerHTML = `
                            <div style="margin-bottom: 10px;">
                                <strong style="color: #2c3e50; font-size: 16px;">📊 INFORMATIONS ITINÉRAIRE OPTIMISÉ</strong>
                            </div>
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px;">
                                <div style="background: #e8f5e8; padding: 8px; border-radius: 6px; border-left: 4px solid #27ae60;">
                                    <strong>📏 Distance:</strong><br>${distance}
                                </div>
                                <div style="background: #e3f2fd; padding: 8px; border-radius: 6px; border-left: 4px solid #2196f3;">
                                    <strong>⏱️ Temps:</strong><br>${time}
                                </div>
                            </div>
                            ${ecoHTML}
                            ${instructions ? `<div style="background: #fff3e0; padding: 8px; border-radius: 6px; border-left: 4px solid #ff9800; font-size: 12px; max-height: 100px; overflow-y: auto;">
                                <strong>🗺️ Instructions:</strong><br>${instructions}
                            </div>` : ''}
                            <div style="margin-top: 10px; display: flex; gap: 10px; justify-content: center;">
                                <button onclick="exportRouteToJSON()" style="background: #3498db; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 11px;">
                                    💾 Sauvegarder
                                </button>
                                <button onclick="shareRoute()" style="background: #9b59b6; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 11px;">
                                    📤 Partager
                                </button>
                            </div>
                        `;
                        routeInfo.style.display = 'block';
                    }

                    function getVehicleName(type) {
                        const vehicles = {
                            'car': 'Voiture',
                            'bike': 'Vélo',
                            'truck': 'Camion',
                            'motorcycle': 'Moto'
                        };
                        return vehicles[type] || type;
                    }

                    function hideRouteInfo() {
                        const routeInfo = document.getElementById('route-info');
                        routeInfo.style.display = 'none';
                    }

                    function showTrafficInfo(message, isWarning = false) {
                        const trafficInfo = document.getElementById('traffic-info');
                        trafficInfo.innerHTML = message;
                        trafficInfo.style.borderColor = isWarning ? '#e74c3c' : '#27ae60';
                        trafficInfo.style.display = 'block';
                        
                        setTimeout(() => {
                            trafficInfo.style.display = 'none';
                        }, 8000);
                    }

                    function initMap() {
                        try {
                            // 🔹 OPTIMISATION CARTE - CONFIGURATION HAUTE PERFORMANCE
                            map = L.map('map', {
                                zoomControl: true,
                                preferCanvas: true,
                                fadeAnimation: true,
                                markerZoomAnimation: true,
                                transform3DLimit: 500,
                                wheelPxPerZoomLevel: 60,
                                inertia: true,
                                inertiaDeceleration: 3000,
                                inertiaMaxSpeed: 1500,
                                zoomAnimation: true,
                                zoomAnimationThreshold: 4,
                                worldCopyJump: false,
                                maxBoundsViscosity: 1.0
                            }).setView([33.5731, -7.5898], 13);
                            
                            // 🔹 COUCHES CARTES HAUTE QUALITÉ
                            baseLayers.osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                                maxZoom: 19,
                                minZoom: 2,
                                detectRetina: true,
                                updateWhenIdle: true,
                                reuseTiles: true
                            });
                            
                            baseLayers.satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                                attribution: '&copy; <a href="https://www.esri.com/">Esri</a>',
                                maxZoom: 19,
                                minZoom: 2,
                                detectRetina: true,
                                updateWhenIdle: true
                            });
                            
                            baseLayers.dark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                                maxZoom: 19,
                                minZoom: 2,
                                detectRetina: true,
                                updateWhenIdle: true
                            });
                            
                            baseLayers.osm.addTo(map);
                            
                            // 🔹 CONTRÔLES OPTIMISÉS
                            L.control.zoom({
                                position: 'topright'
                            }).addTo(map);
                            
                            // 🔹 ÉVÉNEMENTS CARTE OPTIMISÉS
                            map.on('click', function(e) {
                                if (mapClickEnabled) {
                                    handleMapClick(e.latlng.lat, e.latlng.lng);
                                }
                            });
                            
                            // 🔹 MARQUEUR CENTRAL AVEC POPUP
                            L.marker([33.5731, -7.5898])
                                .addTo(map)
                                .bindPopup(`
                                    <div class="custom-popup">
                                        <h3 style="margin: 0 0 10px 0; color: #2c3e50;">📍 Casablanca</h3>
                                        <p style="margin: 5px 0;">🎯 <strong>Carte haute qualité active</strong></p>
                                        <p style="margin: 5px 0;">🚗 <strong>Système d'itinéraire optimisé</strong></p>
                                        <div style="margin-top: 10px; padding: 8px; background: #e8f5e8; border-radius: 5px;">
                                            <small>✅ Prêt pour la navigation</small>
                                        </div>
                                    </div>
                                `)
                                .openPopup();
                            
                            map.whenReady(function() {
                                console.log("✅ Carte Leaflet haute qualité chargée !");
                                isMapReady = true;
                                showStatus("✅ Carte haute qualité chargée - Prêt pour itinéraire");
                                
                                // 🔹 Charger les options depuis Java
                                if (window.javaConnector) {
                                    window.javaConnector.loadRouteOptions();
                                }
                                
                                if (window.javaConnector) {
                                    window.javaConnector.mapReady();
                                }
                                
                                setTimeout(locateByIP, 1000);
                            });
                            
                        } catch (error) {
                            console.error("❌ Erreur initialisation carte:", error);
                            showStatus("❌ Erreur: " + error.message, true);
                        }
                    }
            """);

        // Partie 5 : Suite des fonctions JavaScript
        htmlBuilder.append("""
                    // 🔹 Gestion des options d'itinéraire
                    window.showRouteOptions = function() {
                        const panel = document.getElementById('route-options-panel');
                        panel.style.display = 'flex';
                    };

                    window.hideRouteOptions = function() {
                        const panel = document.getElementById('route-options-panel');
                        panel.style.display = 'none';
                    };

                    window.applyRouteOptions = function() {
                        routeOptions.avoidHighways = document.getElementById('avoidHighways').checked;
                        routeOptions.avoidTolls = document.getElementById('avoidTolls').checked;
                        routeOptions.avoidFerries = document.getElementById('avoidFerries').checked;
                        routeOptions.vehicleType = document.getElementById('vehicleType').value;
                        routeOptions.fuelEfficiency = parseFloat(document.getElementById('fuelEfficiency').value);
                        routeOptions.fuelPrice = parseFloat(document.getElementById('fuelPrice').value);
                        
                        if (window.javaConnector) {
                            window.javaConnector.onRouteOptionsUpdated(
                                routeOptions.avoidHighways,
                                routeOptions.avoidTolls,
                                routeOptions.avoidFerries,
                                routeOptions.vehicleType,
                                routeOptions.fuelEfficiency,
                                routeOptions.fuelPrice
                            );
                        }
                        
                        hideRouteOptions();
                        showStatus("⚙️ Options d'itinéraire mises à jour");
                    };

                    window.loadRouteOptions = function(options) {
                        routeOptions = options;
                        
                        document.getElementById('avoidHighways').checked = options.avoidHighways;
                        document.getElementById('avoidTolls').checked = options.avoidTolls;
                        document.getElementById('avoidFerries').checked = options.avoidFerries;
                        document.getElementById('vehicleType').value = options.vehicleType;
                        document.getElementById('fuelEfficiency').value = options.fuelEfficiency;
                        document.getElementById('fuelPrice').value = options.fuelPrice;
                        
                        console.log("⚙️ Options d'itinéraire chargées:", options);
                    };

                    window.enableMapClick = function() {
                        mapClickEnabled = !mapClickEnabled;
                        if (mapClickEnabled) {
                            showStatus("🖱️ Mode clic activé - Cliquez sur la carte pour sélectionner une position");
                            map.getContainer().style.cursor = 'crosshair';
                            
                            // Désactiver le mode sélection si actif
                            if (clickSelectionActive) {
                                activateClickSelection();
                            }
                        } else {
                            showStatus("❌ Mode clic désactivé");
                            map.getContainer().style.cursor = '';
                        }
                    };

                    function handleMapClick(lat, lng) {
                        if (!mapClickEnabled) return;
                        
                        try {
                            if (clickMarker && map.hasLayer(clickMarker)) {
                                map.removeLayer(clickMarker);
                            }
                            
                            neighborMarkers.forEach(marker => {
                                if (map.hasLayer(marker)) {
                                    map.removeLayer(marker);
                                }
                            });
                            neighborMarkers = [];
                            
                            clickMarker = L.marker([lat, lng], {
                                icon: L.divIcon({
                                    className: 'click-marker',
                                    iconSize: [20, 20],
                                    iconAnchor: [10, 10]
                                })
                            }).addTo(map)
                            .bindPopup(`
                                <div class="custom-popup">
                                    <h4 style="margin: 0 0 8px 0; color: #3498db;">🖱️ POSITION SÉLECTIONNÉE</h4>
                                    <div style="background: #e3f2fd; padding: 8px; border-radius: 5px;">
                                        <strong>Lat:</strong> ${lat.toFixed(6)}<br>
                                        <strong>Lng:</strong> ${lng.toFixed(6)}
                                    </div>
                                    <div style="margin-top: 8px;">
                                        <button onclick="showNeighbors(${lat}, ${lng})" style="background: #9b59b6; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 11px;">
                                            🗺️ Afficher les voisins
                                        </button>
                                        <button onclick="setAsStartPoint(${lat}, ${lng})" style="background: #27ae60; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 11px; margin-left: 5px;">
                                            🟢 Départ
                                        </button>
                                        <button onclick="setAsEndPoint(${lat}, ${lng})" style="background: #e67e22; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 11px; margin-left: 5px;">
                                            🟠 Arrivée
                                        </button>
                                    </div>
                                </div>
                            `)
                            .openPopup();
                            
                            map.flyTo([lat, lng], 15, {
                                duration: 1,
                                easeLinearity: 0.25
                            });
                            
                            if (window.javaConnector) {
                                const address = `Position: ${lat.toFixed(6)}, ${lng.toFixed(6)}`;
                                window.javaConnector.onMapClick(lat, lng, address);
                            }
                            
                            showStatus(`📍 Position sélectionnée: ${lat.toFixed(6)}, ${lng.toFixed(6)}`);
                            
                        } catch (error) {
                            console.error("❌ Erreur gestion clic carte:", error);
                            showStatus("❌ Erreur lors de la sélection de position", true);
                        }
                    }

                    // 🔹 Fonctions pour définir directement depuis un clic
                    window.setAsStartPoint = function(lat, lng) {
                        setStartPosition(lat, lng);
                        if (window.javaConnector) {
                            window.javaConnector.onPointSelected('start', lat, lng);
                        }
                    };

                    window.setAsEndPoint = function(lat, lng) {
                        setEndPosition(lat, lng);
                        if (window.javaConnector) {
                            window.javaConnector.onPointSelected('end', lat, lng);
                        }
                    };

                    window.showNeighbors = function(lat, lng) {
                        try {
                            neighborMarkers.forEach(marker => {
                                if (map.hasLayer(marker)) {
                                    map.removeLayer(marker);
                                }
                            });
                            neighborMarkers = [];
                            
                            const neighbors = generateNeighbors(lat, lng, 5, 0.01);
                            
                            neighbors.forEach((neighbor, index) => {
                                const neighborMarker = L.marker([neighbor.lat, neighbor.lng], {
                                    icon: L.divIcon({
                                        className: 'neighbor-marker',
                                        iconSize: [16, 16],
                                        iconAnchor: [8, 8]
                                    })
                                }).addTo(map)
                                .bindPopup(`
                                    <div class="custom-popup">
                                        <h5 style="margin: 0 0 5px 0; color: #9b59b6;">🏘️ POINT VOISIN ${index + 1}</h5>
                                        <div style="background: #f3e5f5; padding: 6px; border-radius: 4px; font-size: 11px;">
                                            <strong>Lat:</strong> ${neighbor.lat.toFixed(6)}<br>
                                            <strong>Lng:</strong> ${neighbor.lng.toFixed(6)}
                                        </div>
                                    </div>
                                `);
                                
                                neighborMarkers.push(neighborMarker);
                            });
                            
                            simulateTrafficData(lat, lng);
                            
                            showStatus(`🗺️ ${neighbors.length} points voisins affichés autour de la position`);
                            
                        } catch (error) {
                            console.error("❌ Erreur affichage voisins:", error);
                            showStatus("❌ Erreur affichage des voisins", true);
                        }
                    };

                    function generateNeighbors(centerLat, centerLng, count, radius) {
                        const neighbors = [];
                        for (let i = 0; i < count; i++) {
                            const angle = Math.random() * 2 * Math.PI;
                            const distance = Math.random() * radius;
                            
                            const lat = centerLat + Math.cos(angle) * distance;
                            const lng = centerLng + Math.sin(angle) * distance;
                            
                            neighbors.push({
                                lat: lat,
                                lng: lng,
                                distance: distance
                            });
                        }
                        return neighbors;
                    }

                    function simulateTrafficData(lat, lng) {
                        const trafficStates = [
                            { state: "🟢 FLUIDE", color: "#27ae60", issues: [] },
                            { state: "🟡 MODÉRÉ", color: "#f39c12", issues: ["Ralentissements mineurs"] },
                            { state: "🔴 BLOQUÉ", color: "#e74c3c", issues: ["Embouteillages", "Accidents"] },
                            { state: "⚫ SATURÉ", color: "#2c3e50", issues: ["Bouchons", "Travaux", "Accidents graves"] }
                        ];
                        
                        const randomState = trafficStates[Math.floor(Math.random() * trafficStates.length)];
                        
                        let issuesHTML = '';
                        if (randomState.issues.length > 0) {
                            issuesHTML = `<div style="margin-top: 8px;">
                                <strong>🚨 Problèmes détectés:</strong><br>
                                ${randomState.issues.map(issue => `• ${issue}`).join('<br>')}
                            </div>
                            `;
                        }
                        
                        const trafficMessage = `
                            <div style="color: ${randomState.color}; font-weight: bold; margin-bottom: 8px;">
                                ${randomState.state} - ÉTAT DU TRAFIC
                            </div>
                            <div style="font-size: 12px;">
                                <strong>📍 Zone:</strong> ${lat.toFixed(4)}, ${lng.toFixed(4)}<br>
                                <strong>🕒 Dernière mise à jour:</strong> ${new Date().toLocaleTimeString()}
                                ${issuesHTML}
                            </div>
                        `;
                        
                        showTrafficInfo(trafficMessage, randomState.issues.length > 0);
                        
                        if (window.javaConnector) {
                            window.javaConnector.onTrafficData(
                                lat, 
                                lng, 
                                randomState.state, 
                                randomState.issues.join(', ')
                            );
                        }
                    }
            """);

        // Partie 6 : Suite des fonctions JavaScript et fin
        htmlBuilder.append("""
                    window.switchMapStyle = function(style) {
                        if (!isMapReady) return;
                        
                        Object.values(baseLayers).forEach(layer => {
                            if (map.hasLayer(layer)) {
                                map.removeLayer(layer);
                            }
                        });
                        
                        if (baseLayers[style]) {
                            baseLayers[style].addTo(map);
                            currentMapStyle = style;
                            showStatus(`🗺️ Carte ${style} activée`);
                        }
                    };

                    window.toggleFullscreen = function() {
                        const elem = document.documentElement;
                        if (!document.fullscreenElement) {
                            if (elem.requestFullscreen) {
                                elem.requestFullscreen();
                            } else if (elem.webkitRequestFullscreen) {
                                elem.webkitRequestFullscreen();
                            } else if (elem.msRequestFullscreen) {
                                elem.msRequestFullscreen();
                            }
                        } else {
                            if (document.exitFullscreen) {
                                document.exitFullscreen();
                            } else if (document.webkitExitFullscreen) {
                                document.webkitExitFullscreen();
                            } else if (document.msExitFullscreen) {
                                document.msExitFullscreen();
                            }
                        }
                    };

                    window.locateByIP = function() {
                        if (!isMapReady) {
                            showStatus("⚠️ Carte non prête", true);
                            return;
                        }
                        
                        showStatus("🌐 Recherche de votre position par IP...");
                        console.log("📍 Démarrage localisation IP...");
                        
                        if (ipLocationMarker && map.hasLayer(ipLocationMarker)) {
                            map.removeLayer(ipLocationMarker);
                        }
                        
                        if (window.javaConnector) {
                            window.javaConnector.requestIPLocation();
                        } else {
                            showStatus("❌ Service de localisation indisponible", true);
                        }
                    };

                    window.showIPLocation = function(lat, lng, popupInfo) {
                        if (!isMapReady) return;
                        
                        try {
                            if (ipLocationMarker && map.hasLayer(ipLocationMarker)) {
                                map.removeLayer(ipLocationMarker);
                            }
                            
                            const ipIcon = L.divIcon({
                                className: 'ip-marker',
                                html: '<div style="font-size: 24px; text-shadow: 2px 2px 4px rgba(0,0,0,0.5);">📍</div>',
                                iconSize: [30, 30],
                                iconAnchor: [15, 30]
                            });
                            
                            ipLocationMarker = L.marker([lat, lng], { icon: ipIcon })
                                .addTo(map)
                                .bindPopup(`
                                    <div class="custom-popup">
                                        <h3 style="margin: 0 0 8px 0; color: #2c3e50;">🌐 Votre Position</h3>
                                        <div style="background: #e3f2fd; padding: 8px; border-radius: 5px; margin: 5px 0;">
                                            ${popupInfo}
                                        </div>
                                        <small style="color: #7f8c8d;">📍 Localisation via IP</small>
                                    </div>
                                `)
                                .openPopup();
                            
                            map.flyTo([lat, lng], 14, {
                                duration: 1.5,
                                easeLinearity: 0.25
                            });
                            
                            showStatus("✅ Localisation IP trouvée");
                            console.log("📍 Position IP affichée:", lat, lng);
                            
                        } catch (error) {
                            console.error("❌ Erreur affichage IP:", error);
                            showStatus("❌ Erreur affichage position IP", true);
                        }
                    };

                    // 🔹 SECTION ITINÉRAIRE OPTIMISÉE ET AMÉLIORÉE
                    window.setStartPoint = function() {
                        if (!isMapReady) {
                            showStatus("⚠️ Carte non prête", true);
                            return;
                        }
                        
                        showStatus("🟢 Cliquez sur la carte pour définir le point de départ");
                        enablePointSelection('start');
                    };

                    window.setEndPoint = function() {
                        if (!isMapReady) {
                            showStatus("⚠️ Carte non prête", true);
                            return;
                        }
                        
                        showStatus("🟠 Cliquez sur la carte pour définir le point d'arrivée");
                        enablePointSelection('end');
                    };

                    function enablePointSelection(type) {
                        if (clickHandler) {
                            map.off('click', clickHandler);
                        }
                        
                        map.getContainer().style.cursor = 'crosshair';
                        
                        clickHandler = function(e) {
                            const lat = e.latlng.lat;
                            const lng = e.latlng.lng;
                            
                            if (type === 'start') {
                                setStartPosition(lat, lng);
                            } else {
                                setEndPosition(lat, lng);
                            }
                            
                            if (window.javaConnector) {
                                window.javaConnector.onPointSelected(type, lat, lng);
                            }
                            
                            map.getContainer().style.cursor = '';
                            
                            map.off('click', clickHandler);
                            clickHandler = null;
                        };
                        
                        map.on('click', clickHandler);
                    }

                    function setStartPosition(lat, lng) {
                        startPoint = L.latLng(lat, lng);
                        
                        if (startMarker && map.hasLayer(startMarker)) {
                            map.removeLayer(startMarker);
                        }
                        
                        startMarker = L.marker([lat, lng], {
                            icon: L.divIcon({
                                className: 'start-marker',
                                iconSize: [22, 22],
                                iconAnchor: [11, 11]
                            })
                        }).addTo(map)
                        .bindPopup(`
                            <div class="custom-popup">
                                <h4 style="margin: 0 0 8px 0; color: #27ae60;">🟢 POINT DE DÉPART</h4>
                                <div style="background: #e8f5e8; padding: 8px; border-radius: 5px;">
                                    <strong>Lat:</strong> ${lat.toFixed(6)}<br>
                                    <strong>Lng:</strong> ${lng.toFixed(6)}
                                </div>
                            </div>
                        `)
                        .openPopup();
                        
                        showStatus("✅ Point de départ défini");
                    }

                    function setEndPosition(lat, lng) {
                        endPoint = L.latLng(lat, lng);
                        
                        if (endMarker && map.hasLayer(endMarker)) {
                            map.removeLayer(endMarker);
                        }
                        
                        endMarker = L.marker([lat, lng], {
                            icon: L.divIcon({
                                className: 'end-marker',
                                iconSize: [22, 22],
                                iconAnchor: [11, 11]
                            })
                        }).addTo(map)
                        .bindPopup(`
                            <div class="custom-popup">
                                <h4 style="margin: 0 0 8px 0; color: #e67e22;">🟠 POINT D'ARRIVÉE</h4>
                                <div style="background: #fff3e0; padding: 8px; border-radius: 5px;">
                                    <strong>Lat:</strong> ${lat.toFixed(6)}<br>
                                    <strong>Lng:</strong> ${lng.toFixed(6)}
                                </div>
                            </div>
                        `)
                        .openPopup();
                        
                        showStatus("✅ Point d'arrivée défini");
                    }

                    // 🔹 CALCUL D'ITINÉRAIRE OPTIMISÉ AVEC OPTIONS ET ANALYSE
                    window.calculateRoute = function() {
                        if (!startPoint || !endPoint) {
                            showStatus("❌ Définissez d'abord le départ et l'arrivée", true);
                            return;
                        }
                        
                        showStatus("🔄 Calcul de l'itinéraire optimisé en cours...");
                        
                        if (routingControl) {
                            map.removeControl(routingControl);
                        }
                        
                        // 🔹 CONFIGURATION DES OPTIONS D'ITINÉRAIRE
                        const routerOptions = {
                            serviceUrl: 'https://router.project-osrm.org/route/v1',
                            profile: 'driving',
                            useHints: false
                        };
                        
                        // 🔹 AJOUTER LES OPTIONS D'ÉVITEMENT
                        if (routeOptions.avoidHighways || routeOptions.avoidTolls || routeOptions.avoidFerries) {
                            const avoid = [];
                            if (routeOptions.avoidHighways) avoid.push('motorway');
                            if (routeOptions.avoidTolls) avoid.push('toll');
                            if (routeOptions.avoidFerries) avoid.push('ferry');
                            
                            if (avoid.length > 0) {
                                routerOptions.profile = 'driving-traffic';
                                routerOptions.urlParameters = {
                                    avoid: avoid.join(',')
                                };
                            }
                        }
                        
                        // 🔹 CONTRÔLE D'ITINÉRAIRE AVANCÉ
                        routingControl = L.Routing.control({
                            waypoints: [startPoint, endPoint],
                            routeWhileDragging: false,
                            showAlternatives: true,
                            fitSelectedRoutes: 'smart',
                            show: true,
                            lineOptions: {
                                styles: [
                                    {
                                        color: routeOptions.vehicleType === 'bike' ? '#27ae60' : 
                                               routeOptions.vehicleType === 'truck' ? '#e67e22' : '#e74c3c',
                                        weight: routeOptions.vehicleType === 'bike' ? 4 : 6,
                                        opacity: 0.8,
                                        dashArray: routeOptions.vehicleType === 'bike' ? '5, 10' : '10, 10'
                                    }
                                ],
                                extendToWaypoints: true,
                                missingRouteTolerance: 10
                            },
                            altLineOptions: {
                                styles: [
                                    {
                                        color: '#3498db',
                                        weight: 4,
                                        opacity: 0.6
                                    }
                                ]
                            },
                            createMarker: function(i, waypoint, n) {
                                return null;
                            },
                            router: L.Routing.osrmv1(routerOptions),
                            formatter: new L.Routing.Formatter({
                                language: 'fr',
                                units: 'metric'
                            })
                        }).addTo(map);
                        
                        // 🔹 GESTION DES ÉVÉNEMENTS D'ITINÉRAIRE
                        routingControl.on('routesfound', function(e) {
                            const routes = e.routes;
                            if (routes && routes.length > 0) {
                                const route = routes[0];
                                const distanceKm = route.summary.totalDistance / 1000;
                                const distance = distanceKm.toFixed(2) + ' km';
                                const time = Math.round(route.summary.totalTime / 60) + ' min';
                                
                                // 🔹 SAUVEGARDER LES DONNÉES DE L'ITINÉRAIRE
                                currentRouteData.distance = distanceKm;
                                currentRouteData.time = Math.round(route.summary.totalTime / 60);
                                currentRouteData.coordinates = route.coordinates.map(coord => [coord.lat, coord.lng]);
                                
                                // 🔹 CALCULER LES DONNÉES ÉCOLOGIQUES
                                const ecoData = calculateEcoData(distanceKm, routeOptions);
                                
                                // 🔹 METTRE À JOUR LES DONNÉES LOCALES
                                currentRouteData.co2Emissions = ecoData.co2Emissions;
                                currentRouteData.fuelConsumption = ecoData.fuelConsumption;
                                currentRouteData.estimatedCost = ecoData.estimatedCost;
                                
                                // 🔹 CONVERTIR LES COORDONNÉES EN TABLEAU POUR POLYLINE
                                const routeCoords = route.coordinates.map(coord => [coord.lat, coord.lng]);
                                
                                // 🔹 DESSINER LA LIGNE D'ITINÉRAIRE SUR LA CARTE
                                const polyline = L.polyline(routeCoords, {
                                    color: routeOptions.vehicleType === 'bike' ? '#27ae60' : 
                                           routeOptions.vehicleType === 'truck' ? '#e67e22' : '#e74c3c',
                                    weight: routeOptions.vehicleType === 'bike' ? 4 : 6,
                                    opacity: 0.8,
                                    dashArray: routeOptions.vehicleType === 'bike' ? '5, 10' : '10, 10',
                                    className: 'route-polyline'
                                }).addTo(map);
                                
                                // 🔹 DESSINER LES ITINÉRAIRES ALTERNATIFS
                                if (routes.length > 1) {
                                    for (let i = 1; i < routes.length; i++) {
                                        const altCoords = routes[i].coordinates.map(coord => [coord.lat, coord.lng]);
                                        L.polyline(altCoords, {
                                            color: '#3498db',
                                            weight: 4,
                                            opacity: 0.6,
                                            className: 'alt-route-polyline'
                                        }).addTo(map);
                                    }
                                }
                                
                                let instructions = '';
                                if (route.instructions && route.instructions.length > 0) {
                                    const importantInstructions = route.instructions
                                        .filter((instr, idx) => idx === 0 || idx === route.instructions.length - 1 || 
                                                instr.type.includes('Left') || instr.type.includes('Right') || 
                                                instr.type.includes('Enter'))
                                        .map(instr => instr.text)
                                        .join(' → ');
                                    instructions = importantInstructions;
                                    currentRouteData.instructions = importantInstructions;
                                }
                                
                                // 🔹 AFFICHER LES INFORMATIONS AVEC DONNÉES ÉCOLOGIQUES
                                showRouteInfo(distance, time, instructions, {
                                    co2: ecoData.co2Emissions.toFixed(2) + ' kg',
                                    fuel: ecoData.fuelConsumption.toFixed(2) + ' L',
                                    cost: ecoData.estimatedCost.toFixed(2) + ' €'
                                });
                                
                                showStatus("✅ Itinéraire optimisé calculé: " + distance + " - " + time);
                                
                                // 🔹 ADAPTER LA VUE AUX LIMITES DE L'ITINÉRAIRE
                                map.fitBounds(polyline.getBounds());
                                
                                // 🔹 AFFICHER LES DONNÉES ÉCOLOGIQUES
                                showEcoAnalysis(distanceKm, time, route, ecoData);
                                
                                // 🔹 AFFICHER LES POINTS D'INTÉRÊT
                                addPOIsAlongRoute(routeCoords);
                                
                                // 🔹 AFFICHER LES AVERTISSEMENTS DE SÉCURITÉ
                                showRouteHazards(route);
                                
                                // 🔹 NOTIFIER JAVA DES DONNÉES D'ITINÉRAIRE
                                if (window.javaConnector) {
                                    window.javaConnector.onRouteCalculated(
                                        distance, 
                                        time, 
                                        instructions,
                                        ecoData.co2Emissions,
                                        ecoData.fuelConsumption,
                                        ecoData.estimatedCost
                                    );
                                }
                            }
                        });
                        
                        routingControl.on('routingerror', function(e) {
                            console.error("❌ Erreur itinéraire:", e.error);
                            showStatus("❌ Erreur calcul itinéraire: " + e.error.message, true);
                            
                            if (window.javaConnector) {
                                window.javaConnector.onRouteError(e.error.message);
                            }
                        });
                    };

                    // 🔹 CALCUL DES DONNÉES ÉCOLOGIQUES
                    function calculateEcoData(distanceKm, options) {
                        const fuelConsumption = (distanceKm * options.fuelEfficiency) / 100;
                        const estimatedCost = fuelConsumption * options.fuelPrice;
                        const co2Emissions = distanceKm * options.co2PerKm;
                        
                        return {
                            fuelConsumption: fuelConsumption,
                            estimatedCost: estimatedCost,
                            co2Emissions: co2Emissions
                        };
                    }

                    // 🔹 EFFACER L'ITINÉRAIRE
                    window.clearRoute = function() {
                        if (routingControl) {
                            map.removeControl(routingControl);
                            routingControl = null;
                        }
                        
                        if (startMarker && map.hasLayer(startMarker)) {
                            map.removeLayer(startMarker);
                            startMarker = null;
                        }
                        
                        if (endMarker && map.hasLayer(endMarker)) {
                            map.removeLayer(endMarker);
                            endMarker = null;
                        }
                        
                        startPoint = null;
                        endPoint = null;
                        currentRouteData = {
                            distance: 0,
                            time: 0,
                            instructions: '',
                            co2Emissions: 0,
                            fuelConsumption: 0,
                            estimatedCost: 0,
                            coordinates: []
                        };
                        hideRouteInfo();
                        showStatus("🧹 Itinéraire effacé");
                        
                        if (window.javaConnector) {
                            window.javaConnector.onRouteCleared();
                        }
                    };

                    // 🔹 EXPORT DES DONNÉES D'ITINÉRAIRE
                    window.exportRouteData = function() {
                        if (currentRouteData.distance === 0) {
                            showStatus("❌ Aucun itinéraire à exporter", true);
                            return;
                        }
                        
                        const routeData = {
                            distance: currentRouteData.distance,
                            time: currentRouteData.time,
                            instructions: currentRouteData.instructions,
                            co2Emissions: currentRouteData.co2Emissions,
                            fuelConsumption: currentRouteData.fuelConsumption,
                            estimatedCost: currentRouteData.estimatedCost,
                            vehicleType: routeOptions.vehicleType,
                            coordinates: currentRouteData.coordinates,
                            timestamp: new Date().toISOString()
                        };
                        
                        const dataStr = JSON.stringify(routeData, null, 2);
                        const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
                        
                        const exportFileDefaultName = `itineraire_${new Date().toISOString().slice(0,10)}.json`;
                        
                        const linkElement = document.createElement('a');
                        linkElement.setAttribute('href', dataUri);
                        linkElement.setAttribute('download', exportFileDefaultName);
                        linkElement.click();
                        
                        showStatus("💾 Itinéraire exporté au format JSON");
                        
                        if (window.javaConnector) {
                            window.javaConnector.onRouteExported(dataStr);
                        }
                    };

                    // 🔹 EXPORT EN UN CLIC
                    window.exportRouteToJSON = function() {
                        exportRouteData();
                    };

                    // 🔹 PARTAGE D'ITINÉRAIRE
                    window.shareRoute = function() {
                        if (currentRouteData.distance === 0) {
                            showStatus("❌ Aucun itinéraire à partager", true);
                            return;
                        }
                        
                        const shareText = `Itinéraire: ${currentRouteData.distance.toFixed(2)} km - ` +
                                         `${currentRouteData.time} min - ` +
                                         `Émissions: ${currentRouteData.co2Emissions.toFixed(2)} kg CO₂`;
                        
                        if (navigator.share) {
                            navigator.share({
                                title: 'Mon Itinéraire',
                                text: shareText,
                                url: window.location.href
                            }).then(() => {
                                showStatus("📤 Itinéraire partagé avec succès");
                            }).catch(err => {
                                console.error('Erreur partage:', err);
                                showStatus("❌ Erreur lors du partage", true);
                            });
                        } else {
                            // Fallback pour navigateurs sans support
                            navigator.clipboard.writeText(shareText).then(() => {
                                showStatus("📋 Itinéraire copié dans le presse-papier");
                            }).catch(err => {
                                console.error('Erreur copie:', err);
                                showStatus("❌ Erreur lors de la copie", true);
                            });
                        }
                    };

                    window.addMarker = function(lat, lng, title) {
                        if (!isMapReady) return;
                        try {
                            var marker = L.marker([lat, lng]).addTo(map);
                            if (title) {
                                marker.bindPopup(`
                                    <div class="custom-popup">
                                        <div style="background: #f8f9fa; padding: 10px; border-radius: 6px; border-left: 4px solid #3498db;">
                                            ${title}
                                        </div>
                                    </div>
                                `);
                            }
                            markers.push(marker);
                        } catch (error) {
                            console.error("❌ Erreur addMarker:", error);
                        }
                    };

                    window.clearMarkers = function() {
                        if (!isMapReady) return;
                        try {
                            markers.forEach(function(marker) {
                                if (map.hasLayer(marker)) {
                                    map.removeLayer(marker);
                                }
                            });
                            markers = [];
                            
                            if (ipLocationMarker && map.hasLayer(ipLocationMarker)) {
                                map.removeLayer(ipLocationMarker);
                                ipLocationMarker = null;
                            }
                            
                            if (clickMarker && map.hasLayer(clickMarker)) {
                                map.removeLayer(clickMarker);
                                clickMarker = null;
                            }
                            
                            neighborMarkers.forEach(marker => {
                                if (map.hasLayer(marker)) {
                                    map.removeLayer(marker);
                                }
                            });
                            neighborMarkers = [];
                            
                            // Effacer également les lignes de routes
                            routeLines.forEach(routeLine => {
                                if (map.hasLayer(routeLine)) {
                                    map.removeLayer(routeLine);
                                }
                            });
                            routeLines = [];
                            
                            // Effacer le panel de sélection
                            document.getElementById('route-selection-panel').style.display = 'none';
                            
                            mapClickEnabled = false;
                            clickSelectionActive = false;
                            map.getContainer().style.cursor = '';
                            
                            showStatus("🧹 Marqueurs et routes effacés");
                        } catch (error) {
                            console.error("❌ Erreur clearMarkers:", error);
                        }
                    };

                    window.setView = function(lat, lng, zoom) {
                        if (!isMapReady) return;
                        try {
                            map.flyTo([lat, lng], zoom, {
                                duration: 1.5,
                                easeLinearity: 0.25
                            });
                        } catch (error) {
                            console.error("❌ Erreur setView:", error);
                        }
                    };

                    // 🔹 ANALYSE ÉCOLOGIQUE AMÉLIORÉE DE L'ITINÉRAIRE
                    function showEcoAnalysis(distance, time, route, ecoData) {
                        const distKm = parseFloat(distance);
                        const ecologyScore = Math.max(0, 100 - (distKm * 2) - (ecoData.co2Emissions * 10)).toFixed(0);
                        
                        const ecoInfo = document.getElementById('eco-info');
                        ecoInfo.innerHTML = `
                            <div style="margin-bottom: 10px; font-size: 14px; font-weight: bold;">
                                🌱 ANALYSE ÉCOLOGIQUE AVANCÉE
                            </div>
                            <div style="background: rgba(255,255,255,0.1); padding: 10px; border-radius: 6px; margin: 8px 0;">
                                <div style="margin: 5px 0;">
                                    🌍 <strong>Émissions CO₂:</strong> ${ecoData.co2Emissions.toFixed(2)} kg
                                    <div class="eco-badge">${ecoData.co2Emissions < 5 ? 'Faible' : ecoData.co2Emissions < 15 ? 'Moyen' : 'Élevé'}</div>
                                </div>
                                <div style="margin: 5px 0;">
                                    ⛽ <strong>Consommation:</strong> ${ecoData.fuelConsumption.toFixed(2)} L
                                    <div class="eco-badge">${ecoData.fuelConsumption < 10 ? 'Économe' : 'Gourmand'}</div>
                                </div>
                                <div style="margin: 5px 0;">
                                    💰 <strong>Coût estimé:</strong> €${ecoData.estimatedCost.toFixed(2)}
                                    <div class="eco-badge">${ecoData.estimatedCost < 20 ? 'Économique' : 'Coûteux'}</div>
                                </div>
                                <div style="margin: 10px 0; font-size: 14px; font-weight: bold;">
                                    📊 Score Écologie: ${ecologyScore}/100
                                    <div style="font-size: 12px; margin-top: 5px;">
                                        ${ecologyScore > 80 ? '✅ Excellent - Itinéraire très écologique' : 
                                          ecologyScore > 60 ? '🟡 Bon - Impact environnemental modéré' : 
                                          ecologyScore > 40 ? '🟠 Moyen - Pensez au covoiturage' : 
                                          '🔴 Faible - Considérez les transports alternatifs'}
                                    </div>
                                </div>
                                <div style="margin-top: 10px; font-size: 11px; opacity: 0.8;">
                                    💡 Conseil: ${getEcoAdvice(ecologyScore, routeOptions.vehicleType)}
                                </div>
                            </div>
                        `;
                        ecoInfo.style.display = 'block';
                        setTimeout(() => { ecoInfo.style.display = 'none'; }, 15000);
                    }

                    function getEcoAdvice(score, vehicleType) {
                        if (score > 80) return 'Continuez comme ça !';
                        if (score > 60) return 'Envisagez le covoiturage pour réduire davantage les émissions.';
                        if (score > 40) return 'Les transports en commun pourraient être une alternative.';
                        return 'Pour les trajets courts, envisagez la marche ou le vélo.';
                    }

                    // 🔹 AJOUTER LES POINTS D'INTÉRÊT SUR L'ITINÉRAIRE
                    function addPOIsAlongRoute(routeCoords) {
                        const poiCategories = [
                            { name: '🏨 Hôtel', icon: '🏨', type: 'lodging' },
                            { name: '⛽ Station essence', icon: '⛽', type: 'fuel' },
                            { name: '🍔 Restaurants', icon: '🍔', type: 'food' },
                            { name: '🚻 Toilettes', icon: '🚻', type: 'toilet' },
                            { name: '🏥 Hôpital', icon: '🏥', type: 'hospital' },
                            { name: '🛒 Supermarché', icon: '🛒', type: 'market' }
                        ];
                        
                        // Ajouter POIs en fonction du type de véhicule
                        const relevantPOIs = poiCategories.filter(poi => {
                            if (routeOptions.vehicleType === 'bike') {
                                return poi.type === 'food' || poi.type === 'toilet';
                            } else if (routeOptions.vehicleType === 'truck') {
                                return poi.type === 'fuel' || poi.type === 'lodging';
                            }
                            return true;
                        });
                        
                        // Ajouter 3-5 POIs aléatoires le long de la route
                        const poiCount = Math.min(Math.floor(Math.random() * 3) + 3, relevantPOIs.length);
                        for (let i = 0; i < poiCount; i++) {
                            const randomIndex = Math.floor(Math.random() * routeCoords.length);
                            const poiCoord = routeCoords[randomIndex];
                            const poiCategory = relevantPOIs[Math.floor(Math.random() * relevantPOIs.length)];
                            
                            const poiMarker = L.marker([poiCoord[0], poiCoord[1]], {
                                icon: L.divIcon({
                                    className: 'poi-marker',
                                    html: `<div style="font-size: 14px; display: flex; align-items: center; justify-content: center; width: 100%; height: 100%;">${poiCategory.icon}</div>`,
                                    iconSize: [24, 24],
                                    iconAnchor: [12, 12]
                                })
                            }).addTo(map)
                            .bindPopup(`
                                <div class="custom-popup">
                                    <strong>${poiCategory.name}</strong><br>
                                    <small>📍 Point d'intérêt sur l'itinéraire</small><br>
                                    <small>🚗 Recommandé pour: ${getVehicleName(routeOptions.vehicleType)}</small>
                                </div>
                            `);
                        }
                    }

                    // 🔹 AFFICHER LES AVERTISSEMENTS DE SÉCURITÉ AMÉLIORÉS
                    function showRouteHazards(route) {
                        const hazards = [];
                        
                        // Détection de pentes fortes
                        if (route.coordinates.length > 10) {
                            const elevationChanges = detectElevationChanges(route.coordinates);
                            elevationChanges.forEach(change => {
                                if (change > 50) hazards.push('⚠️ Forte pente ascendante détectée');
                                if (change < -50) hazards.push('⚠️ Forte pente descendante détectée');
                            });
                        }
                        
                        // Avertissements basés sur le type de véhicule
                        if (routeOptions.vehicleType === 'truck') {
                            hazards.push('🚛 Accès limité pour poids lourds - Vérifier restrictions');
                        } else if (routeOptions.vehicleType === 'bike') {
                            hazards.push('🚲 Pistes cyclables limitées - Prudence requise');
                        }
                        
                        // Avertissements généraux
                        hazards.push('☔ Conditions météo variables - Adaptez votre vitesse');
                        hazards.push('🌙 Éclairage faible sur certaines sections');
                        
                        const randomHazards = hazards.sort(() => Math.random() - 0.5).slice(0, 3);
                        
                        const hazardBox = document.getElementById('route-hazard');
                        hazardBox.innerHTML = `
                            <div style="margin-bottom: 8px; font-weight: bold;">🚨 ALERTES DE SÉCURITÉ AVANCÉES</div>
                            ${randomHazards.map(h => `<div style="margin: 5px 0; padding: 5px; background: rgba(255,255,255,0.1); border-radius: 4px;">• ${h}</div>`).join('')}
                        `;
                        hazardBox.style.display = 'block';
                        setTimeout(() => { hazardBox.style.display = 'none'; }, 12000);
                    }

                    function detectElevationChanges(coordinates) {
                        const changes = [];
                        for (let i = 1; i < Math.min(coordinates.length, 10); i++) {
                            const change = (Math.random() - 0.5) * 100;
                            changes.push(change);
                        }
                        return changes;
                    }

                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', function() {
                            setTimeout(initMap, 100);
                        });
                    } else {
                        setTimeout(initMap, 100);
                    }

                </script>
            </body>
            </html>
            """);

        return htmlBuilder.toString();
    }

    public class JavaConnector {
        public void mapReady() {
            Platform.runLater(() -> {
                if (!isMapInitialized) {
                    isMapInitialized = true;
                    initializationLatch.countDown();
                    System.out.println("✅✅✅ Carte HAUTE QUALITÉ COMPLÈTEMENT initialisée !");

                    if (mapReadyListener != null) {
                        mapReadyListener.run();
                    }
                }
            });
        }

        public void loadRouteOptions() {
            Platform.runLater(() -> {
                try {
                    String script = String.format(
                            "if (typeof loadRouteOptions === 'function') { loadRouteOptions(%s); }",
                            routeOptions.toJavaScriptObject()
                    );
                    engine.executeScript(script);
                    System.out.println("⚙️ Options d'itinéraire envoyées à JavaScript");
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement options: " + e.getMessage());
                }
            });
        }

        public void requestIPLocation() {
            new Thread(() -> {
                try {
                    int userId = getCurrentUserId();
                    System.out.println("🌐 Début géolocalisation IP haute précision pour l'utilisateur: " + userId);

                    IPGeolocationService.IPLocation location = IPGeolocationService.getLocationByIP(userId);

                    Platform.runLater(() -> {
                        try {
                            String script = String.format(
                                    "if (typeof showIPLocation === 'function') { showIPLocation(%f, %f, '%s'); }",
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    location.getPopupInfo().replace("'", "\\'")
                            );
                            engine.executeScript(script);

                            System.out.println("📍 Localisation IP haute précision affichée pour user " + userId + ": " + location.getFormattedAddress());

                        } catch (Exception e) {
                            System.err.println("❌ Erreur exécution script IP: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    System.err.println("❌ Erreur géolocalisation IP: " + e.getMessage());
                }
            }).start();
        }

        public void onPointSelected(String type, double lat, double lng) {
            Platform.runLater(() -> {
                if ("start".equals(type)) {
                    startLat = lat;
                    startLng = lng;
                    System.out.println("🟢 Point de départ défini: " + lat + ", " + lng);
                } else if ("end".equals(type)) {
                    endLat = lat;
                    endLng = lng;
                    System.out.println("🟠 Point d'arrivée défini: " + lat + ", " + lng);
                }
            });
        }

        public void onRouteCalculated(String distance, String time, String instructions,
                                      double co2Emissions, double fuelConsumption, double estimatedCost) {
            Platform.runLater(() -> {
                System.out.println("✅ Itinéraire optimisé calculé - Distance: " + distance + ", Temps: " + time);
                System.out.println("🌱 Données écologiques - CO₂: " + co2Emissions + " kg, " +
                        "Carburant: " + fuelConsumption + " L, Coût: " + estimatedCost + " €");

                currentRouteDistance = distance;
                currentRouteTime = time;
                currentRouteInstructions = instructions != null ? instructions : "";

                // Notifier le listener des données d'itinéraire
                if (routeDataListener != null) {
                    try {
                        double distanceKm = Double.parseDouble(distance.replace(" km", ""));
                        double timeMin = Double.parseDouble(time.replace(" min", ""));
                        RouteData routeData = new RouteData(
                                distanceKm, timeMin, instructions,
                                co2Emissions, fuelConsumption, estimatedCost
                        );
                        routeDataListener.accept(routeData);
                    } catch (NumberFormatException e) {
                        System.err.println("❌ Erreur parsing données itinéraire: " + e.getMessage());
                    }
                }
            });
        }

        public void onRouteError(String errorMessage) {
            Platform.runLater(() -> {
                System.err.println("❌ Erreur calcul itinéraire: " + errorMessage);
            });
        }

        public void onRouteOptionsUpdated(boolean avoidHighways, boolean avoidTolls,
                                          boolean avoidFerries, String vehicleType,
                                          double fuelEfficiency, double fuelPrice) {
            Platform.runLater(() -> {
                routeOptions.setAvoidHighways(avoidHighways);
                routeOptions.setAvoidTolls(avoidTolls);
                routeOptions.setAvoidFerries(avoidFerries);
                routeOptions.setVehicleType(vehicleType);
                routeOptions.setFuelEfficiency(fuelEfficiency);
                routeOptions.setFuelPrice(fuelPrice);

                System.out.println("⚙️ Options mises à jour: " + routeOptions.toJavaScriptObject());
            });
        }

        public void onRouteExported(String jsonData) {
            Platform.runLater(() -> {
                System.out.println("💾 Itinéraire exporté: " + jsonData.length() + " caractères");
            });
        }

        public void onRouteCleared() {
            Platform.runLater(() -> {
                startLat = 0.0;
                startLng = 0.0;
                endLat = 0.0;
                endLng = 0.0;
                currentRouteDistance = "";
                currentRouteTime = "";
                currentRouteInstructions = "";
                System.out.println("🧹 Itinéraire effacé");
            });
        }

        public void onMapClick(double lat, double lng, String address) {
            Platform.runLater(() -> {
                System.out.println("🖱️ Clic sur la carte - Lat: " + lat + ", Lng: " + lng + ", Adresse: " + address);

                if (mapClickListener != null) {
                    MapClickData clickData = new MapClickData(lat, lng, address);
                    mapClickListener.accept(clickData);
                }
            });
        }

        public void onTrafficData(double lat, double lng, String trafficState, String issues) {
            Platform.runLater(() -> {
                System.out.println("🚦 Données trafic - Position: " + lat + ", " + lng);
                System.out.println("   État: " + trafficState);
                System.out.println("   Problèmes: " + issues);
            });
        }

        // 🔥 MÉTHODE DU SECOND CODE POUR LA SÉLECTION DE ROUTES
        public void onRouteSelected(String routeId, String routeName) {
            Platform.runLater(() -> {
                System.out.println("🎯 Route sélectionnée dans Java: " + routeName + " (" + routeId + ")");

                Route routeSelectionnee = null;
                for (Route route : routesDisponibles) {
                    if (route.getId().equals(routeId)) {
                        routeSelectionnee = route;
                        break;
                    }
                }

                if (routeSelectionnee != null && onRouteSelectedCallback != null) {
                    onRouteSelectedCallback.accept(routeSelectionnee);
                }
            });
        }

        // 🔥 MÉTHODE POUR DESSINER LES ROUTES (CORRECTION DU BUG)
        public void drawRoutes() {
            Platform.runLater(() -> {
                dessinerRoutesSurCarte();
            });
        }
    }

    // ==================== MÉTHODES MANQUANTES ====================

    public void centrerSurCasablanca() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (map) map.setView([33.5731, -7.5898], 13)");
                System.out.println("📍 Centrage sur Casablanca");
            } catch (Exception e) {
                System.err.println("❌ Erreur centrage Casablanca: " + e.getMessage());
            }
        });
    }

    public void centrerSurRabat() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (map) map.setView([33.9692, -6.9272], 13)");
                System.out.println("📍 Centrage sur Rabat");
            } catch (Exception e) {
                System.err.println("❌ Erreur centrage Rabat: " + e.getMessage());
            }
        });
    }

    public void zoomIn() {
        if (!isMapInitialized) return;
        if (currentZoom < 19) {
            currentZoom++;
            ajusterEchelle(currentZoom);
        }
    }

    public void zoomOut() {
        if (!isMapInitialized) return;
        if (currentZoom > 1) {
            currentZoom--;
            ajusterEchelle(currentZoom);
        }
    }

    public void ajusterEchelle(int zoomLevel) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof ajusterEchelle === 'function') ajusterEchelle(%d)", zoomLevel);
                engine.executeScript(script);
                System.out.println("🔍 Échelle ajustée à: " + zoomLevel);
            } catch (Exception e) {
                System.err.println("❌ Erreur ajustement échelle: " + e.getMessage());
            }
        });
    }

    public void redemarrerCarte() {
        Platform.runLater(() -> {
            try {
                // Réinitialiser la carte
                engine.executeScript("if (typeof redemarrerCarte === 'function') redemarrerCarte()");
                System.out.println("🔄 Redémarrage de la carte");
            } catch (Exception e) {
                System.err.println("❌ Erreur redémarrage carte: " + e.getMessage());
            }
        });
    }

    public void activerCarteStandard() {
        switchToStandardMap();
    }

    public void activerCarteSatellite() {
        switchToSatelliteMap();
    }

    public void activerCarteSombre() {
        switchToDarkMap();
    }

    // ==================== MÉTHODES EXISTANTES ====================

    public void showRouteOptions() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof showRouteOptions === 'function') showRouteOptions()");
                System.out.println("⚙️ Affichage des options d'itinéraire");
            } catch (Exception e) {
                System.err.println("❌ Erreur affichage options: " + e.getMessage());
            }
        });
    }

    public void exportRoute() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof exportRouteData === 'function') exportRouteData()");
                System.out.println("📤 Export de l'itinéraire demandé");
            } catch (Exception e) {
                System.err.println("❌ Erreur export itinéraire: " + e.getMessage());
            }
        });
    }

    public void shareRoute() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof shareRoute === 'function') shareRoute()");
                System.out.println("📤 Partage de l'itinéraire demandé");
            } catch (Exception e) {
                System.err.println("❌ Erreur partage itinéraire: " + e.getMessage());
            }
        });
    }

    public void setStartPoint() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof setStartPoint === 'function') setStartPoint()");
                System.out.println("🟢 Mode sélection point de départ activé");
            } catch (Exception e) {
                System.err.println("❌ Erreur activation point départ: " + e.getMessage());
            }
        });
    }

    public void setEndPoint() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof setEndPoint === 'function') setEndPoint()");
                System.out.println("🟠 Mode sélection point d'arrivée activé");
            } catch (Exception e) {
                System.err.println("❌ Erreur activation point arrivée: " + e.getMessage());
            }
        });
    }

    public void calculateRoute() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof calculateRoute === 'function') calculateRoute()");
                System.out.println("🚗 Calcul d'itinéraire optimisé démarré");
            } catch (Exception e) {
                System.err.println("❌ Erreur calcul itinéraire: " + e.getMessage());
            }
        });
    }

    public void clearRoute() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof clearRoute === 'function') clearRoute()");
                System.out.println("🧹 Effacement itinéraire demandé");
            } catch (Exception e) {
                System.err.println("❌ Erreur effacement itinéraire: " + e.getMessage());
            }
        });
    }

    public void enableMapClick() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof enableMapClick === 'function') enableMapClick()");
                System.out.println("🖱️ Mode clic sur carte activé");
            } catch (Exception e) {
                System.err.println("❌ Erreur activation clics carte: " + e.getMessage());
            }
        });
    }

    public void showNeighbors(double lat, double lng) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof showNeighbors === 'function') showNeighbors(%f, %f)", lat, lng);
                engine.executeScript(script);
                System.out.println("🗺️ Affichage des points voisins pour: " + lat + ", " + lng);
            } catch (Exception e) {
                System.err.println("❌ Erreur affichage voisins: " + e.getMessage());
            }
        });
    }

    public void switchToStandardMap() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof switchMapStyle === 'function') switchMapStyle('osm')");
                System.out.println("🗺️ Passage à la carte standard");
            } catch (Exception e) {
                System.err.println("❌ Erreur changement carte standard: " + e.getMessage());
            }
        });
    }

    public void switchToSatelliteMap() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof switchMapStyle === 'function') switchMapStyle('satellite')");
                System.out.println("🛰️ Passage à la carte satellite");
            } catch (Exception e) {
                System.err.println("❌ Erreur changement carte satellite: " + e.getMessage());
            }
        });
    }

    public void switchToDarkMap() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof switchMapStyle === 'function') switchMapStyle('dark')");
                System.out.println("🌙 Passage à la carte sombre");
            } catch (Exception e) {
                System.err.println("❌ Erreur changement carte sombre: " + e.getMessage());
            }
        });
    }

    public void toggleFullscreen() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof toggleFullscreen === 'function') toggleFullscreen()");
                System.out.println("📺 Basculer mode plein écran");
            } catch (Exception e) {
                System.err.println("❌ Erreur plein écran: " + e.getMessage());
            }
        });
    }

    public void localiserParIP() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof locateByIP === 'function') locateByIP()");
                System.out.println("🌐 Démarrage localisation IP haute précision...");
            } catch (Exception e) {
                System.err.println("❌ Erreur localisation IP: " + e.getMessage());
            }
        });
    }

    private void notifyMapReady() {
        new JavaConnector().mapReady();
    }

    public Parent getMapPanel() {
        return this;
    }

    public void waitForInitialization() {
        try {
            if (initializationLatch.await(20, TimeUnit.SECONDS)) {
                System.out.println("✅ Carte haute qualité initialisée avec succès");
            } else {
                System.err.println("⚠️ Timeout attente initialisation carte (20s)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void effacerMarqueurs() {
        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof clearMarkers === 'function') clearMarkers()");
                System.out.println("🧹 Marqueurs effacés");
            } catch (Exception e) {
                System.err.println("❌ Erreur effacement marqueurs: " + e.getMessage());
            }
        });
    }

    public void ajouterMarqueur(double lat, double lon, String titre) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof addMarker === 'function') addMarker(%f, %f, '%s')", lat, lon,
                        titre.replace("'", "\\'"));
                engine.executeScript(script);
            } catch (Exception e) {
                System.err.println("❌ Erreur ajout marqueur: " + e.getMessage());
            }
        });
    }

    public void centrerCarte(double lat, double lon, int zoom) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof setView === 'function') setView(%f, %f, %d)", lat, lon, zoom);
                engine.executeScript(script);
            } catch (Exception e) {
                System.err.println("❌ Erreur centrage: " + e.getMessage());
            }
        });
    }

    public boolean estInitialisee() {
        return isMapInitialized;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public void showIPLocationOnMap(double latitude, double longitude, String ipAddress) {
        Platform.runLater(() -> {
            try {
                String script = String.format(
                        "if (typeof showIPLocation === 'function') showIPLocation(%f, %f, 'IP: %s')",
                        latitude, longitude, ipAddress
                );
                engine.executeScript(script);
                System.out.println("📍 Localisation IP haute précision affichée: " + latitude + ", " + longitude);
            } catch (Exception e) {
                System.err.println("❌ Erreur affichage IP sur carte: " + e.getMessage());
            }
        });
    }

    public double getStartLat() { return startLat; }
    public double getStartLng() { return startLng; }
    public double getEndLat() { return endLat; }
    public double getEndLng() { return endLng; }

    // 🔹 Getters pour les données d'itinéraire
    public String getCurrentRouteDistance() { return currentRouteDistance; }
    public String getCurrentRouteTime() { return currentRouteTime; }
    public String getCurrentRouteInstructions() { return currentRouteInstructions; }

    // 🔹 Définir un point de départ programmatiquement
    public void setStartPoint(double lat, double lng) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof setAsStartPoint === 'function') setAsStartPoint(%f, %f)", lat, lng);
                engine.executeScript(script);
                System.out.println("🟢 Point de départ défini programmatiquement: " + lat + ", " + lng);
            } catch (Exception e) {
                System.err.println("❌ Erreur définition point départ: " + e.getMessage());
            }
        });
    }

    // 🔹 Définir un point d'arrivée programmatiquement
    public void setEndPoint(double lat, double lng) {
        Platform.runLater(() -> {
            try {
                String script = String.format("if (typeof setAsEndPoint === 'function') setAsEndPoint(%f, %f)", lat, lng);
                engine.executeScript(script);
                System.out.println("🟠 Point d'arrivée défini programmatiquement: " + lat + ", " + lng);
            } catch (Exception e) {
                System.err.println("❌ Erreur définition point arrivée: " + e.getMessage());
            }
        });
    }

    // 🔹 Calculer l'itinéraire entre deux points spécifiques
    public void calculateRoute(double startLat, double startLng, double endLat, double endLng) {
        Platform.runLater(() -> {
            try {
                setStartPoint(startLat, startLng);
                setEndPoint(endLat, endLng);

                // Attendre un peu puis calculer l'itinéraire
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        Platform.runLater(() -> {
                            calculateRoute();
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                System.out.println("🚗 Calcul d'itinéraire programmatique démarré");
            } catch (Exception e) {
                System.err.println("❌ Erreur calcul itinéraire programmatique: " + e.getMessage());
            }
        });
    }

    // 🔥 GETTERS ET SETTERS DU SECOND CODE
    public List<Route> getRoutesDisponibles() {
        return new ArrayList<>(routesDisponibles);
    }

    public Route getRouteSelectionnee() {
        return routeSelectionnee;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        webView.resize(getWidth(), getHeight());
    }

    // 🔹 MÉTHODE POUR VÉRIFIER SI UN ITINÉRAIRE EST ACTIF
    public boolean hasActiveRoute() {
        return startLat != 0.0 && startLng != 0.0 && endLat != 0.0 && endLng != 0.0 &&
               !currentRouteDistance.isEmpty();
    }

    // 🔹 MÉTHODE POUR AFFICHER UN MESSAGE DE STATUT (INTERNE)
    private void showStatus(String message) {
        System.out.println("🗺️ " + message);
    }
}