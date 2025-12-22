package com.example.demo.view;

import com.example.demo.model.FeuCirculation;
import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RouteDetailsView extends BorderPane {
    private Route route;
    private RouteCanvas routeCanvas;
    private Label statusLabel;
    private Button btnSimuler;
    private AnimationTimer simulationTimer;
    private Timeline animationFeux;
    private boolean simulationEnCours = false;

    // 🔥 PHYSICS ENGINE VARIABLES
    private Map<String, Double> vehicleProgressions; // Progression individuelle (0-1)
    private Map<String, Double> vehicleVitesses; // Vitesse actuelle (km/h)
    private Map<String, Double> vehicleAccelerations; // Accélération (km/h²)
    private Map<String, Double> vehicleDistancesSecurite; // Distance de sécurité
    private Map<String, Long> vehicleStopTimesRealistic; // Temps d'arrêt réaliste
    private Map<String, Boolean> vehicleEnCollision; // État de collision
    private Random random = ThreadLocalRandom.current();

    // 🔥 ENVIRONMENTAL FACTORS
    private double trafficDensity = 0.7; // Densité du trafic (0-1)
    private double weatherFactor = 1.0; // Facteur météo (pluie = 0.8, neige = 0.6)
    private double timeOfDayFactor = 1.0; // Facteur heure de la journée
    private boolean rushHour = false; // Heure de pointe

    public RouteDetailsView(Route route) {
        System.out.println("🚀 RouteDetailsView créé avec route: " + route.getNom());
        System.out.println("🚗 Véhicules dans la route: " + route.getVehicules().size());

        this.route = route;
        initialiserUI();
    }

    private void initialiserUI() {
        setStyle("-fx-background-color: #f8f9fa;");

        // En-tête
        VBox header = creerEnTete();
        setTop(header);

        // Canvas pour le dessin de la route
        routeCanvas = new RouteCanvas(800, 500);
        routeCanvas.setRoute(route);

        // 🔥 RouteCanvas gère maintenant l'animation des feux en interne

        ScrollPane canvasContainer = new ScrollPane(routeCanvas);
        canvasContainer.setFitToWidth(true);
        canvasContainer.setFitToHeight(true);
        canvasContainer.setStyle("-fx-background: white; -fx-border-color: #ddd;");

        setCenter(canvasContainer);

        // Contrôles
        HBox controls = creerControles();
        setBottom(controls);

        // Status
        statusLabel = new Label("✅ Route chargée - Prêt pour simulation");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
        BorderPane.setMargin(statusLabel, new Insets(5));
        setLeft(statusLabel);
    }

    private VBox creerEnTete() {
        Label titre = new Label("📊 Détails de la Route: " + route.getNom());
        titre.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titre.setTextFill(Color.DARKBLUE);

        String infosText = String.format(
                "🛣️ %d points • 📏 %s km • 🚗 %d véhicules • 🏙️ Environnement urbain",
                route.getPoints().size(),
                calculerLongueurEstimee(),
                route.getVehicules().size()
        );

        Label info = new Label(infosText);
        info.setFont(Font.font("Arial", 14));
        info.setTextFill(Color.DARKGRAY);

        VBox header = new VBox(10, titre, info);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0;");

        return header;
    }

    private HBox creerControles() {
        // Boutons de zoom
        Button btnZoomIn = new Button("🔍 Zoom +");
        btnZoomIn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnZoomIn.setOnAction(e -> {
            routeCanvas.zoomIn();
            updateStatus("Zoom avant");
        });

        Button btnZoomOut = new Button("🔍 Zoom -");
        btnZoomOut.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        btnZoomOut.setOnAction(e -> {
            routeCanvas.zoomOut();
            updateStatus("Zoom arrière");
        });

        Button btnReset = new Button("🔄 Vue initiale");
        btnReset.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        btnReset.setOnAction(e -> {
            routeCanvas.resetView();
            updateStatus("Vue réinitialisée");
        });

        // 🔥 BOUTON SIMULATION AMÉLIORÉ
        btnSimuler = new Button("🚗 Démarrer Simulation");
        btnSimuler.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSimuler.setOnAction(e -> {
            if (!simulationEnCours) {
                demarrerSimulation();
            } else {
                arreterSimulation();
            }
        });

        Button btnRetour = new Button("← Retour à la Carte");
        btnRetour.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        btnRetour.setOnAction(e -> {
            arreterSimulation();
            retourCarte();
        });

        HBox controls = new HBox(15, btnZoomIn, btnZoomOut, btnReset, btnSimuler, btnRetour);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 2 0 0 0;");

        return controls;
    }

    // 🔥 MÉTHODES PUBLIC POUR LE CONTROLEUR - AJOUTÉES ICI
    public void demarrerSimulationAutomatique() {
        System.out.println("🚀 Démarrage automatique de la simulation depuis le contrôleur");
        if (!simulationEnCours) {
            demarrerSimulation();
        }
    }

    public void arreterSimulationAutomatique() {
        System.out.println("🛑 Arrêt automatique de la simulation depuis le contrôleur");
        if (simulationEnCours) {
            arreterSimulation();
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Démarrer la simulation
    private void demarrerSimulation() {
        System.out.println("🚀 Démarrage de la simulation pour: " + route.getNom());

        simulationEnCours = true;
        btnSimuler.setText("⏹ Arrêter Simulation");
        btnSimuler.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");

        // Démarrer la simulation sur le canvas (RouteCanvas gère maintenant tout)
        routeCanvas.demarrerSimulation();
        updateStatus("🚗 Simulation en cours...");
    }

    // 🔥 NOUVELLE MÉTHODE: Arrêter la simulation
    private void arreterSimulation() {
        System.out.println("🛑 Arrêt de la simulation");

        simulationEnCours = false;
        btnSimuler.setText("🚗 Démarrer Simulation");
        btnSimuler.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");

        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        routeCanvas.arreterSimulation();
        updateStatus("✅ Simulation arrêtée");
    }



    private String calculerLongueurEstimee() {
        if (route.getPoints().size() < 2) return "0.0";

        double distance = 0;
        for (int i = 0; i < route.getPoints().size() - 1; i++) {
            Route.Point p1 = route.getPoints().get(i);
            Route.Point p2 = route.getPoints().get(i + 1);

            double latDiff = p2.getLatitude() - p1.getLatitude();
            double lngDiff = p2.getLongitude() - p1.getLongitude();
            distance += Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
        }  
        return String.format("%.1f", distance * 110);
    }

    private void retourCarte() {
        System.out.println("🗺️ Retour à la carte principale");
        // Arrêter les animations avant de fermer
        if (simulationEnCours) {
            arreterSimulation();
        }
        if (animationFeux != null) {
            animationFeux.stop();
        }
        getScene().getWindow().hide();
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("📢 " + message);
        }
    }

    // 🔥 MÉTHODE DE NETTOYAGE - OPTIONNELLE MAIS RECOMMANDÉE
    public void cleanup() {
        if (simulationEnCours) {
            arreterSimulation();
        }
        if (animationFeux != null) {
            animationFeux.stop();
        }
        if (routeCanvas != null) {
            // Si RouteCanvas a une méthode de nettoyage
            try {
                routeCanvas.getClass().getMethod("cleanup").invoke(routeCanvas);
            } catch (Exception e) {
                // Ignorer si la méthode n'existe pas
            }
        }
    }
}