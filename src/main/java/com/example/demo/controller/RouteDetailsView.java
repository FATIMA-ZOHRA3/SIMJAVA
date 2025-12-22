package com.example.demo.controller;

import com.example.demo.model.FeuCirculation;
import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import com.example.demo.view.RouteCanvas;
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

public class RouteDetailsView extends BorderPane {
    private Route route;
    private RouteCanvas routeCanvas;
    private Label statusLabel;
    private Button btnSimuler;
    private AnimationTimer simulationTimer;
    private boolean simulationEnCours = false;
    
    // 🔥 VARIABLES POUR LA CIRCULATION RÉALISTE
    private Map<String, Long> vehicleStopTimes; // Temps d'arrêt des véhicules
    private Map<String, Double> vehicleProgressions; // Progression individuelle
    private Map<String, Double> vehicleVitesses; // Vitesse individuelle
    private Random random = new Random();
    private double trafficDensity = 0.7; // Densité du trafic (0-1)

    public RouteDetailsView(Route route) {
        System.out.println("🚀 RouteDetailsView créé avec route: " + route.getNom());
        System.out.println("🚗 Véhicules dans la route: " + route.getVehicules().size());

        this.route = route;
        initialiserUI();
        initialiserCirculation();
    }

    // 🔥 NOUVELLE MÉTHODE: Initialiser la circulation réaliste
    private void initialiserCirculation() {
        vehicleStopTimes = new HashMap<>();
        vehicleProgressions = new HashMap<>();
        vehicleVitesses = new HashMap<>();
        
        // Initialiser chaque véhicule avec des paramètres réalistes
        for (Vehicle vehicule : route.getVehicules()) {
            String vehicleId = vehicule.getId();
            
            // Progression aléatoire pour éviter le départ groupé
            vehicleProgressions.put(vehicleId, random.nextDouble());
            
            // Vitesse réaliste selon le type de véhicule
            double vitesseBase = getVitesseBase(vehicule.getType());
            double variation = (random.nextDouble() - 0.5) * 20; // ±10 km/h
            vehicleVitesses.put(vehicleId, Math.max(30, vitesseBase + variation));
            
            vehicleStopTimes.put(vehicleId, 0L);
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Obtenir la vitesse de base selon le type
    private double getVitesseBase(String type) {
        switch (type.toLowerCase()) {
            case "camion":
                return 70.0; // Camions plus lents
            case "bus":
                return 60.0; // Bus modérés
            case "voiture":
            default:
                return 80.0; // Voitures plus rapides
        }
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

        // 🔥 AMÉLIORATION: Panneau de contrôle de la circulation
        HBox trafficControls = creerControlesCirculation();
        setRight(trafficControls);

        // Contrôles principaux
        HBox controls = creerControles();
        setBottom(controls);

        // Status
        statusLabel = new Label("✅ Route chargée - Prêt pour simulation réaliste");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
        BorderPane.setMargin(statusLabel, new Insets(5));
        setLeft(statusLabel);
    }

    // 🔥 NOUVELLE MÉTHODE: Contrôles de la circulation
    private HBox creerControlesCirculation() {
        VBox trafficPanel = new VBox(10);
        trafficPanel.setPadding(new Insets(15));
        trafficPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 0 2;");
        trafficPanel.setPrefWidth(200);

        Label trafficTitle = new Label("🚦 Contrôle Circulation");
        trafficTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        trafficTitle.setTextFill(Color.DARKBLUE);

        // Contrôle de densité
        Label densityLabel = new Label("Densité du trafic: " + (int)(trafficDensity * 100) + "%");
        Button btnPlusDense = new Button("➕");
        Button btnMoinsDense = new Button("➖");
        
        btnPlusDense.setOnAction(e -> {
            trafficDensity = Math.min(1.0, trafficDensity + 0.1);
            densityLabel.setText("Densité du trafic: " + (int)(trafficDensity * 100) + "%");
            ajusterDensiteTrafic();
        });
        
        btnMoinsDense.setOnAction(e -> {
            trafficDensity = Math.max(0.1, trafficDensity - 0.1);
            densityLabel.setText("Densité du trafic: " + (int)(trafficDensity * 100) + "%");
            ajusterDensiteTrafic();
        });

        HBox densityControls = new HBox(5, btnMoinsDense, btnPlusDense);
        densityControls.setAlignment(Pos.CENTER);

        // Statistiques en temps réel
        Label statsLabel = new Label("📊 Stats en direct");
        statsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label vehiclesMovingLabel = new Label("🚗 Véhicules en mouvement: 0");
        Label vehiclesStoppedLabel = new Label("🛑 Véhicules arrêtés: 0");
        Label avgSpeedLabel = new Label("📏 Vitesse moyenne: 0 km/h");

        trafficPanel.getChildren().addAll(
            trafficTitle, densityLabel, densityControls,
            new Label(" "), statsLabel, vehiclesMovingLabel, 
            vehiclesStoppedLabel, avgSpeedLabel
        );

        // 🔥 Mise à jour des statistiques en temps réel
        Timeline statsUpdater = new Timeline(
            new KeyFrame(Duration.seconds(2), e -> updateStats(vehiclesMovingLabel, vehiclesStoppedLabel, avgSpeedLabel))
        );
        statsUpdater.setCycleCount(Timeline.INDEFINITE);
        statsUpdater.play();

        return new HBox(trafficPanel);
    }

    // 🔥 NOUVELLE MÉTHODE: Mettre à jour les statistiques
    private void updateStats(Label movingLabel, Label stoppedLabel, Label speedLabel) {
        if (!simulationEnCours) return;

        int moving = 0;
        int stopped = 0;
        double totalSpeed = 0;
        int count = 0;

        for (Vehicle vehicule : route.getVehicules()) {
            String vehicleId = vehicule.getId();
            if (vehicleStopTimes.get(vehicleId) > 0) {
                stopped++;
            } else {
                moving++;
                totalSpeed += vehicleVitesses.get(vehicleId);
                count++;
            }
        }

        double avgSpeed = count > 0 ? totalSpeed / count : 0;

        movingLabel.setText("🚗 Véhicules en mouvement: " + moving);
        stoppedLabel.setText("🛑 Véhicules arrêtés: " + stopped);
        speedLabel.setText("📏 Vitesse moyenne: " + (int)avgSpeed + " km/h");
    }

    // 🔥 NOUVELLE MÉTHODE: Ajuster la densité du trafic
    private void ajusterDensiteTrafic() {
        if (!simulationEnCours) return;

        // Ajuster les vitesses en fonction de la densité
        for (Vehicle vehicule : route.getVehicules()) {
            String vehicleId = vehicule.getId();
            double vitesseBase = getVitesseBase(vehicule.getType());
            double reduction = (1 - trafficDensity) * 40; // Réduction jusqu'à 40 km/h
            double nouvelleVitesse = Math.max(20, vitesseBase - reduction);
            vehicleVitesses.put(vehicleId, nouvelleVitesse);
        }

        updateStatus("🔄 Densité ajustée: " + (int)(trafficDensity * 100) + "%");
    }

    private VBox creerEnTete() {
        Label titre = new Label("🚗 Simulation de Circulation Réaliste: " + route.getNom());
        titre.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titre.setTextFill(Color.DARKBLUE);

        String infosText = String.format(
            "🛣️ %d points • 📏 %s km • 🚗 %d véhicules • 🏙️ Environnement urbain complet",
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

        // 🔥 BOUTON SIMULATION RÉALISTE
        btnSimuler = new Button("🚦 Démarrer Circulation");
        btnSimuler.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSimuler.setOnAction(e -> {
            if (!simulationEnCours) {
                demarrerSimulationRealiste();
            } else {
                arreterSimulation();
            }
        });

        Button btnAccident = new Button("🚨 Simuler Accident");
        btnAccident.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white;");
        btnAccident.setOnAction(e -> simulerAccident());

        Button btnRetour = new Button("← Retour à la Carte");
        btnRetour.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        btnRetour.setOnAction(e -> {
            arreterSimulation();
            retourCarte();
        });

        HBox controls = new HBox(10, btnZoomIn, btnZoomOut, btnReset, btnSimuler, btnAccident, btnRetour);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 2 0 0 0;");

        return controls;
    }

    // 🔥 NOUVELLE MÉTHODE: Démarrer la simulation réaliste
    private void demarrerSimulationRealiste() {
        System.out.println("🚀 Démarrage de la simulation réaliste pour: " + route.getNom());

        simulationEnCours = true;
        btnSimuler.setText("⏹ Arrêter Circulation");
        btnSimuler.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");

        // Réinitialiser la circulation
        initialiserCirculation();

        routeCanvas.setRoute(route);
        routeCanvas.demarrerSimulation();
        updateStatus("🚗 Circulation réaliste en cours...");

        // Animation des véhicules avec comportements réalistes
        demarrerAnimationRealiste();
    }

    // 🔥 NOUVELLE MÉTHODE: Animation réaliste
    private void demarrerAnimationRealiste() {
        simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private long lastTrafficChange = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                // 🔥 VARIATIONS ALÉATOIRES DU TRAFIC
                if (now - lastTrafficChange > 5_000_000_000L) { // Toutes les 5 secondes
                    simulerVariationsTrafic();
                    lastTrafficChange = now;
                }

                // Mettre à jour les positions des véhicules
                mettreAJourCirculationRealiste(elapsedSeconds);
            }
        };

        simulationTimer.start();
    }

    // 🔥 NOUVELLE MÉTHODE: Variations aléatoires du trafic
    private void simulerVariationsTrafic() {
        // Changements aléatoires de vitesse
        for (Vehicle vehicule : route.getVehicules()) {
            String vehicleId = vehicule.getId();
            double currentSpeed = vehicleVitesses.get(vehicleId);
            double variation = (random.nextDouble() - 0.5) * 10; // ±5 km/h
            double newSpeed = Math.max(20, Math.min(120, currentSpeed + variation));
            vehicleVitesses.put(vehicleId, newSpeed);
        }

        // Arrêts aléatoires (embouteillages, etc.)
        if (random.nextDouble() < 0.1) { // 10% de chance
            int randomVehicle = random.nextInt(route.getVehicules().size());
            String vehicleId = route.getVehicules().get(randomVehicle).getId();
            vehicleStopTimes.put(vehicleId, System.nanoTime());
            System.out.println("🚧 Véhicule " + vehicleId + " bloqué dans un embouteillage");
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Mettre à jour la circulation réaliste
    private void mettreAJourCirculationRealiste(double elapsedSeconds) {
        if (route.getPoints().size() < 2) return;

        List<Vehicle> vehicules = route.getVehicules();
        if (vehicules.isEmpty()) return;

        long currentTime = System.nanoTime();

        for (int i = 0; i < vehicules.size(); i++) {
            Vehicle vehicule = vehicules.get(i);
            String vehicleId = vehicule.getId();

            // 🔥 GESTION DES ARRÊTS
            boolean isStopped = vehicleStopTimes.get(vehicleId) > 0;
            if (isStopped) {
                double stopDuration = (currentTime - vehicleStopTimes.get(vehicleId)) / 1_000_000_000.0;
                
                // Durée d'arrêt variable selon la cause
                double stopTime = getDureeArret(vehicleId);
                if (stopDuration >= stopTime) {
                    vehicleStopTimes.put(vehicleId, 0L);
                    System.out.println("🚗 Véhicule " + vehicleId + " reprend sa route après " + stopDuration + "s");
                } else {
                    continue; // Rester arrêté
                }
            }

            // 🔥 PROGRESSION INDIVIDUELLE
            double progression = vehicleProgressions.get(vehicleId);
            double vitesse = vehicleVitesses.get(vehicleId);
            
            // Convertir la vitesse en progression (km/h -> progression par seconde)
            double progressionParSeconde = (vitesse / 3600.0) / getLongueurReelle() * 1000;
            progression += progressionParSeconde * elapsedSeconds;

            // Gestion du dépassement de fin de route
            if (progression >= 1.0) {
                progression = 0.0; // Retour au début
                // Nouvelle vitesse aléatoire
                double nouvelleVitesse = getVitesseBase(vehicule.getType()) + (random.nextDouble() - 0.5) * 20;
                vehicleVitesses.put(vehicleId, nouvelleVitesse);
            }

            vehicleProgressions.put(vehicleId, progression);

            // 🔥 DÉTECTION DES FEUX ROUGES
            if (estPresDeFeuRouge(progression)) {
                vehicleStopTimes.put(vehicleId, currentTime);
                System.out.println("🛑 Véhicule " + vehicleId + " s'arrête au feu rouge");
                continue;
            }

            // 🔥 DÉTECTION DES COLLISIONS (distance de sécurité)
            if (risqueCollision(vehicleId, progression)) {
                vehicleStopTimes.put(vehicleId, currentTime);
                System.out.println("⚠️ Véhicule " + vehicleId + " freine pour éviter une collision");
                continue;
            }

            // 🔥 CALCUL DE LA POSITION
            int segmentIndex = (int) (progression * (route.getPoints().size() - 1));
            double ratioDansSegment = progression * (route.getPoints().size() - 1) - segmentIndex;

            if (segmentIndex < route.getPoints().size() - 1) {
                Route.Point p1 = route.getPoints().get(segmentIndex);
                Route.Point p2 = route.getPoints().get(segmentIndex + 1);

                double lat = p1.getLatitude() + (p2.getLatitude() - p1.getLatitude()) * ratioDansSegment;
                double lng = p1.getLongitude() + (p2.getLongitude() - p1.getLongitude()) * ratioDansSegment;

                vehicule.setPosition(lat, lng);
            }
        }

        // 🔥 RouteCanvas gère maintenant la mise à jour des véhicules en interne
    }

    // 🔥 NOUVELLE MÉTHODE: Durée d'arrêt variable
    private double getDureeArret(String vehicleId) {
        // Feux rouges: 30 secondes
        if (estPresDeFeuRouge(vehicleProgressions.get(vehicleId))) {
            return 30.0;
        }
        // Embouteillages: 5-15 secondes
        return 5 + random.nextDouble() * 10;
    }

    // 🔥 NOUVELLE MÉTHODE: Détection des feux rouges
    private boolean estPresDeFeuRouge(double progression) {
        List<FeuCirculation> feux = route.getFeuxCirculation();
        if (feux.isEmpty()) return false;

        for (FeuCirculation feu : feux) {
            // Position approximative du feu sur la route
            double feuPosition = getPositionFeu(feu);
            double distance = Math.abs(progression - feuPosition);
            
            // Zone d'arrêt avant le feu
            if (distance < 0.05 && "rouge".equalsIgnoreCase(feu.getEtat())) {
                return true;
            }
        }
        return false;
    }

    // 🔥 NOUVELLE MÉTHODE: Risque de collision
    private boolean risqueCollision(String currentVehicleId, double currentProgression) {
        double distanceSecurite = 0.02; // Distance de sécurité

        for (Vehicle otherVehicle : route.getVehicules()) {
            String otherId = otherVehicle.getId();
            if (otherId.equals(currentVehicleId)) continue;

            Double otherProgression = vehicleProgressions.get(otherId);
            if (otherProgression == null) continue;

            // Vérifier la distance entre les véhicules
            double distance = Math.abs(currentProgression - otherProgression);
            if (distance < distanceSecurite && currentProgression > otherProgression) {
                return true; // Risque de collision par l'arrière
            }
        }
        return false;
    }

    // 🔥 NOUVELLE MÉTHODE: Obtenir la position d'un feu
    private double getPositionFeu(FeuCirculation feu) {
        // Répartir les feux uniformément sur la route
        int feuIndex = route.getFeuxCirculation().indexOf(feu);
        return (feuIndex + 1.0) / (route.getFeuxCirculation().size() + 1.0);
    }

    // 🔥 NOUVELLE MÉTHODE: Simuler un accident
    private void simulerAccident() {
        if (!simulationEnCours) return;

        // Choisir un véhicule au hasard
        int accidentVehicle = random.nextInt(route.getVehicules().size());
        String vehicleId = route.getVehicules().get(accidentVehicle).getId();

        // Arrêter le véhicule
        vehicleStopTimes.put(vehicleId, System.nanoTime());

        // Ralentir les véhicules autour
        double accidentPosition = vehicleProgressions.get(vehicleId);
        for (Vehicle vehicule : route.getVehicules()) {
            String otherId = vehicule.getId();
            if (!otherId.equals(vehicleId)) {
                double otherPosition = vehicleProgressions.get(otherId);
                double distance = Math.abs(otherPosition - accidentPosition);
                
                if (distance < 0.1) { // Dans un rayon de 10%
                    // Ralentir considérablement
                    vehicleVitesses.put(otherId, 10.0); // 10 km/h
                    vehicleStopTimes.put(otherId, System.nanoTime() - (long)(2_000_000_000L)); // Arrêt de 2 secondes
                }
            }
        }

        updateStatus("🚨 Accident simulé! Circulation perturbée");
        System.out.println("🚨 Accident simulé avec le véhicule " + vehicleId);
    }

    // 🔥 NOUVELLE MÉTHODE: Obtenir la longueur réelle de la route
    private double getLongueurReelle() {
        return Double.parseDouble(calculerLongueurEstimee().replace(" km", ""));
    }

    // ... (les autres méthodes restent similaires mais utilisent la nouvelle logique)

    private void arreterSimulation() {
        System.out.println("🛑 Arrêt de la simulation réaliste");

        simulationEnCours = false;
        btnSimuler.setText("🚦 Démarrer Circulation");
        btnSimuler.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");

        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        routeCanvas.arreterSimulation();
        updateStatus("✅ Circulation arrêtée");
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

        return String.format("%.1f km", distance * 110);
    }

    private void retourCarte() {
        System.out.println("🗺️ Retour à la carte principale");
        getScene().getWindow().hide();
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("📢 " + message);
        }
    }
}