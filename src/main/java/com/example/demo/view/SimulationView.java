package com.example.demo.view;

import com.example.demo.controller.SimulationManager;
import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class SimulationView extends BorderPane {

    private SimulationManager simulationManager;
    private Route routeSelectionnee;
    private Canvas simulationCanvas;
    private GraphicsContext gc;
    private AnimationTimer animationTimer;
    private Runnable onBackRequest;
    private Random random = new Random();
    
    // Variables pour une simulation réaliste
    private double simulationSpeed = 1.0; // Facteur de vitesse (0.5x à 2.0x)
    private boolean isPaused = false;
    private long simulationTime = 0; // Temps simulé en millisecondes
    private List<VehicleState> vehicleStates = new ArrayList<>();
    
    // Classes internes pour la gestion réaliste des véhicules
    private class VehicleState {
        Vehicle vehicle;
        double currentPosition; // Position sur la route (0.0 à 1.0)
        double currentSpeed; // Vitesse actuelle (km/h)
        double targetSpeed; // Vitesse cible (km/h)
        int currentSegment; // Segment actuel sur la route
        double progressInSegment; // Progression dans le segment (0.0 à 1.0)
        Color vehicleColor; // Couleur aléatoire pour le véhicule
        boolean isBraking = false;
        double brakingDistance = 0;
        double followingDistance = 50; // Distance de sécurité en mètres
        VehicleState vehicleAhead = null;
        
        VehicleState(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.currentPosition = random.nextDouble() * 0.8; // Position aléatoire initiale
            this.currentSpeed = vehicle.getVitesse();
            this.targetSpeed = vehicle.getVitesse();
            this.vehicleColor = generateRandomColor();
            this.currentSegment = 0;
            this.progressInSegment = 0;
        }
    }
    
    public SimulationView(SimulationManager simulationManager, Route route) {
        this.simulationManager = simulationManager;
        this.routeSelectionnee = route;
        
        // Initialiser les états des véhicules
        for (Vehicle vehicle : route.getVehicules()) {
            vehicleStates.add(new VehicleState(vehicle));
        }
        
        initializeUI();
        startSimulation();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Header avec informations de la route
        VBox header = createHeader();
        setTop(header);

        // Zone de simulation principale avec Canvas
        simulationCanvas = new Canvas(1000, 600); // Taille augmentée pour plus de réalisme
        gc = simulationCanvas.getGraphicsContext2D();
        
        Pane canvasContainer = new Pane(simulationCanvas);
        canvasContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #0f3460, #1a1a2e);");
        canvasContainer.setMinSize(1000, 600);
        canvasContainer.setPrefSize(1000, 600);
        
        // Lier la taille du canvas
        simulationCanvas.widthProperty().bind(canvasContainer.widthProperty());
        simulationCanvas.heightProperty().bind(canvasContainer.heightProperty());
        setCenter(canvasContainer);

        // Contrôles en bas avec plus d'options
        VBox bottomControls = createEnhancedControls();
        setBottom(bottomControls);

        System.out.println("🚗 SimulationView optimisée créée pour: " + routeSelectionnee.getNom());
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #0f3460, #533483);");
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🚗 SIMULATION DE TRAFIC RÉALISTE");
        titleLabel.setFont(Font.font("Arial", 28));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0.5, 0, 1);");

        Label routeLabel = new Label("📍 Route: " + routeSelectionnee.getNom());
        routeLabel.setFont(Font.font("Arial", 18));
        routeLabel.setTextFill(Color.LIGHTGRAY);

        HBox statsBox = new HBox(30);
        statsBox.setAlignment(Pos.CENTER);
        
        Label vehiclesLabel = createStatLabel("🚗 Véhicules: " + routeSelectionnee.getVehicules().size(), "#4ecdc4");
        Label lengthLabel = createStatLabel("📏 Longueur: " + calculateRouteLength() + " km", "#45b7d1");
        Label avgSpeedLabel = createStatLabel("⚡ Vitesse moy: " + calculateAverageSpeed() + " km/h", "#96ceb4");
        
        statsBox.getChildren().addAll(vehiclesLabel, lengthLabel, avgSpeedLabel);

        header.getChildren().addAll(titleLabel, routeLabel, statsBox);
        return header;
    }
    
    private Label createStatLabel(String text, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.web(color));
        label.setStyle("-fx-font-weight: bold; -fx-padding: 5px 15px; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10px;");
        return label;
    }

    private VBox createEnhancedControls() {
        VBox controlsContainer = new VBox(15);
        controlsContainer.setPadding(new Insets(15));
        controlsContainer.setStyle("-fx-background-color: linear-gradient(to top, #1a1a2e, #16213e);");
        controlsContainer.setAlignment(Pos.CENTER);

        // Contrôles principaux
        HBox mainControls = new HBox(20);
        mainControls.setAlignment(Pos.CENTER);
        
        Button pauseButton = new Button("⏸️ Pause");
        styleControlButton(pauseButton, "#f39c12");
        pauseButton.setOnAction(e -> togglePause());

        Button speedUpButton = new Button("⏩ Accélérer (x" + String.format("%.1f", simulationSpeed + 0.5) + ")");
        styleControlButton(speedUpButton, "#3498db");
        speedUpButton.setOnAction(e -> adjustSpeed(0.5));

        Button speedDownButton = new Button("⏪ Ralentir (x" + String.format("%.1f", simulationSpeed - 0.5) + ")");
        styleControlButton(speedDownButton, "#3498db");
        speedDownButton.setOnAction(e -> adjustSpeed(-0.5));

        Button resetButton = new Button("🔄 Réinitialiser");
        styleControlButton(resetButton, "#9b59b6");
        resetButton.setOnAction(e -> resetSimulation());

        Button backButton = new Button("← Retour à la carte");
        styleControlButton(backButton, "#e74c3c");
        backButton.setOnAction(e -> {
            if (onBackRequest != null) {
                simulationManager.arreterSimulation();
                onBackRequest.run();
            }
        });

        mainControls.getChildren().addAll(pauseButton, speedUpButton, speedDownButton, resetButton, backButton);

        // Contrôle de vitesse
        HBox speedControl = new HBox(10);
        speedControl.setAlignment(Pos.CENTER);
        
        Label speedLabel = new Label("Vitesse simulation: x" + String.format("%.1f", simulationSpeed));
        speedLabel.setTextFill(Color.WHITE);
        speedLabel.setFont(Font.font("Arial", 12));
        
        Slider speedSlider = new Slider(0.1, 3.0, simulationSpeed);
        speedSlider.setPrefWidth(200);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = newVal.doubleValue();
            speedLabel.setText("Vitesse simulation: x" + String.format("%.1f", simulationSpeed));
        });
        
        speedControl.getChildren().addAll(speedLabel, speedSlider);

        // Informations temps réel
        HBox realtimeInfo = new HBox(20);
        realtimeInfo.setAlignment(Pos.CENTER);
        
        Label timeLabel = new Label("⏱️ Temps simulé: 00:00");
        timeLabel.setTextFill(Color.LIGHTGRAY);
        timeLabel.setFont(Font.font("Arial", 12));
        
        Label trafficLabel = new Label("🚦 État trafic: Normal");
        trafficLabel.setTextFill(Color.LIGHTGRAY);
        trafficLabel.setFont(Font.font("Arial", 12));
        
        realtimeInfo.getChildren().addAll(timeLabel, trafficLabel);

        controlsContainer.getChildren().addAll(mainControls, speedControl, realtimeInfo);
        
        // Mettre à jour les labels en temps réel
        AnimationTimer infoUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long minutes = (simulationTime / 60000) % 60;
                long seconds = (simulationTime / 1000) % 60;
                timeLabel.setText(String.format("⏱️ Temps simulé: %02d:%02d", minutes, seconds));
                
                // Calculer l'état du trafic
                double avgSpeed = vehicleStates.stream()
                    .mapToDouble(vs -> vs.currentSpeed)
                    .average()
                    .orElse(0);
                
                String trafficState;
                if (avgSpeed < 20) {
                    trafficState = "🚨 Bouchon";
                    trafficLabel.setTextFill(Color.RED);
                } else if (avgSpeed < 40) {
                    trafficState = "⚠️ Ralenti";
                    trafficLabel.setTextFill(Color.ORANGE);
                } else {
                    trafficState = "✅ Fluide";
                    trafficLabel.setTextFill(Color.LIGHTGREEN);
                }
                trafficLabel.setText("🚦 État trafic: " + trafficState);
            }
        };
        infoUpdater.start();

        return controlsContainer;
    }
    
    private void styleControlButton(Button button, String color) {
        button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, %s, darken(%s, 20%%));
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px 20px; 
            -fx-font-size: 13px; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 30%%);
            -fx-border-width: 1px;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.3, 0, 2);
        """, color, color, color));
        
        button.setOnMouseEntered(e -> button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, lighten(%s, 10%%), %s);
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px 20px; 
            -fx-font-size: 13px; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 20%%);
            -fx-border-width: 1px;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0.4, 0, 3);
        """, color, color, color)));
        
        button.setOnMouseExited(e -> button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, %s, darken(%s, 20%%));
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px 20px; 
            -fx-font-size: 13px; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 30%%);
            -fx-border-width: 1px;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.3, 0, 2);
        """, color, color, color)));
    }

    private void startSimulation() {
        simulationManager.demarrerSimulation(routeSelectionnee.getId());
        startRendering();
    }
    
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused && animationTimer != null) {
            animationTimer.stop();
        } else if (!isPaused && animationTimer != null) {
            animationTimer.start();
        }
    }
    
    private void adjustSpeed(double delta) {
        simulationSpeed = Math.max(0.1, Math.min(3.0, simulationSpeed + delta));
    }
    
    private void resetSimulation() {
        simulationTime = 0;
        vehicleStates.clear();
        for (Vehicle vehicle : routeSelectionnee.getVehicules()) {
            vehicleStates.add(new VehicleState(vehicle));
        }
    }

    private void startRendering() {
        if (animationTimer != null) {
            animationTimer.stop();
        }

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
                if (!isPaused) {
                    simulationTime += (long)(elapsedSeconds * 1000 * simulationSpeed);
                    updateVehicles(elapsedSeconds * simulationSpeed);
                }
                
                renderSimulation();
            }
        };
        animationTimer.start();
    }
    
    private void updateVehicles(double elapsedSeconds) {
        List<Route.Point> routePoints = routeSelectionnee.getPoints();
        if (routePoints.isEmpty()) return;
        
        for (VehicleState vs : vehicleStates) {
            // Mettre à jour le véhicule devant
            updateVehicleAhead(vs);
            
            // Gestion réaliste de la vitesse
            if (vs.vehicleAhead != null) {
                // Calculer la distance avec le véhicule devant
                double distance = calculateDistanceBetween(vs, vs.vehicleAhead);
                
                // Si trop proche, réduire la vitesse
                if (distance < vs.followingDistance) {
                    vs.targetSpeed = Math.max(20, vs.vehicleAhead.currentSpeed - 10);
                    vs.isBraking = true;
                    vs.brakingDistance = distance;
                } else {
                    vs.targetSpeed = vs.vehicle.getVitesse();
                    vs.isBraking = false;
                }
            } else {
                vs.targetSpeed = vs.vehicle.getVitesse();
                vs.isBraking = false;
            }
            
            // Simulation réaliste d'accélération/décélération
            double acceleration = vs.isBraking ? -15 : 5; // m/s²
            double speedDiff = vs.targetSpeed - vs.currentSpeed;
            
            if (Math.abs(speedDiff) > 0.1) {
                double change = acceleration * elapsedSeconds;
                if (Math.abs(change) > Math.abs(speedDiff)) {
                    vs.currentSpeed = vs.targetSpeed;
                } else {
                    vs.currentSpeed += (speedDiff > 0 ? change : -change);
                }
            }
            
            // Calculer la distance parcourue (convertir km/h en m/s puis en fraction de route)
            double speedMps = vs.currentSpeed / 3.6; // Convertir en m/s
            double routeLengthMeters = calculateRouteLength() * 1000; // km en mètres
            double distanceFraction = (speedMps * elapsedSeconds) / routeLengthMeters;
            
            // Mettre à jour la position
            vs.currentPosition = (vs.currentPosition + distanceFraction) % 1.0;
            
            // Ajouter des variations aléatoires réalistes
            if (random.nextDouble() < 0.01) { // 1% de chance par frame
                vs.currentSpeed += (random.nextDouble() - 0.5) * 5; // Variation de ±2.5 km/h
                vs.currentSpeed = Math.max(20, Math.min(120, vs.currentSpeed));
            }
        }
    }
    
    private void updateVehicleAhead(VehicleState currentVs) {
        double minDistance = Double.MAX_VALUE;
        VehicleState closestAhead = null;
        
        for (VehicleState otherVs : vehicleStates) {
            if (otherVs == currentVs) continue;
            
            // Calculer la distance entre les véhicules sur la route
            double distance = (otherVs.currentPosition - currentVs.currentPosition) % 1.0;
            if (distance < 0) distance += 1.0; // Normaliser
            
            if (distance > 0 && distance < minDistance && distance < 0.1) { // Seulement si dans les 10% devant
                minDistance = distance;
                closestAhead = otherVs;
            }
        }
        
        currentVs.vehicleAhead = closestAhead;
    }
    
    private double calculateDistanceBetween(VehicleState vs1, VehicleState vs2) {
        double routeLength = calculateRouteLength() * 1000; // en mètres
        double positionDiff = (vs2.currentPosition - vs1.currentPosition) % 1.0;
        if (positionDiff < 0) positionDiff += 1.0;
        return positionDiff * routeLength;
    }

    private void renderSimulation() {
        // Clear avec dégradé pour un effet de ciel
        drawSkyBackground();
        
        // Dessiner la route réaliste
        drawRealisticRoad();
        
        // Afficher les véhicules
        drawVehicles();
        
        // Afficher les informations et HUD
        drawHUD();
        
        // Effets spéciaux (soleil, ombres)
        drawSpecialEffects();
    }
    
    private void drawSkyBackground() {
        // Dégradé du ciel
        Color topColor = Color.rgb(135, 206, 235, 0.8);
        Color bottomColor = Color.rgb(30, 40, 60, 1.0);
        gc.setFill(new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, null, 
            new javafx.scene.paint.Stop(0, topColor),
            new javafx.scene.paint.Stop(1, bottomColor)));
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());
        
        // Nuages
        drawClouds();
    }
    
    private void drawClouds() {
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        for (int i = 0; i < 5; i++) {
            double x = (simulationTime / 10000.0 + i * 0.2) % 1.0 * simulationCanvas.getWidth();
            double y = 50 + i * 30;
            drawCloud(x, y, 60 + i * 10);
        }
    }
    
    private void drawCloud(double x, double y, double size) {
        gc.setFill(Color.rgb(255, 255, 255, 0.4));
        gc.fillOval(x, y, size, size * 0.6);
        gc.fillOval(x + size * 0.3, y - size * 0.2, size * 0.8, size * 0.5);
        gc.fillOval(x - size * 0.3, y, size * 0.7, size * 0.4);
    }
    
    private void drawRealisticRoad() {
        double centerY = simulationCanvas.getHeight() * 0.6;
        double roadWidth = simulationCanvas.getWidth() * 0.8;
        double startX = (simulationCanvas.getWidth() - roadWidth) / 2;
        double endX = startX + roadWidth;
        
        // Chaussée principale
        gc.setFill(Color.rgb(50, 50, 50));
        gc.fillRoundRect(startX - 10, centerY - 25, roadWidth + 20, 50, 20, 20);
        
        // Bande d'arrêt d'urgence
        gc.setFill(Color.rgb(100, 100, 100));
        gc.fillRect(startX - 10, centerY - 25, 10, 50);
        gc.fillRect(endX, centerY - 25, 10, 50);
        
        // Lignes de séparation (effet de mouvement)
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(3);
        double dashOffset = (simulationTime / 50.0) % 40;
        
        for (double x = startX; x < endX; x += 40) {
            double lineX = x - dashOffset;
            if (lineX >= startX && lineX <= endX - 20) {
                gc.strokeLine(lineX, centerY, lineX + 20, centerY);
            }
        }
        
        // Bords de route
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(startX, centerY - 25, endX, centerY - 25);
        gc.strokeLine(startX, centerY + 25, endX, centerY + 25);
        
        // Marques kilométriques
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 10));
        for (int km = 0; km < (int)calculateRouteLength(); km++) {
            double kmX = startX + (km / calculateRouteLength()) * roadWidth;
            if (kmX > startX && kmX < endX) {
                gc.fillText(km + "km", kmX, centerY - 40);
                gc.fillRect(kmX - 1, centerY - 35, 2, 10);
            }
        }
    }
    
    private void drawVehicles() {
        double centerY = simulationCanvas.getHeight() * 0.6;
        double roadWidth = simulationCanvas.getWidth() * 0.8;
        double startX = (simulationCanvas.getWidth() - roadWidth) / 2;
        
        for (VehicleState vs : vehicleStates) {
            // Position X basée sur la position dans la route
            double vehicleX = startX + vs.currentPosition * roadWidth;
            
            // Légère variation de position Y pour les voies
            double laneOffset = (vs.vehicle.hashCode() % 3 - 1) * 8; // -8, 0, ou +8
            double vehicleY = centerY + laneOffset;
            
            // Taille réaliste basée sur le type de véhicule
            double width, height;
            switch (vs.vehicle.getType()) {
                case "Camion":
                    width = 70;
                    height = 30;
                    break;
                case "Bus":
                    width = 65;
                    height = 28;
                    break;
                default:
                    width = 50;
                    height = 25;
            }
            
            // Dessiner le véhicule avec effet 3D
            drawRealisticVehicle(vehicleX, vehicleY, width, height, vs);
            
            // Ombre
            drawVehicleShadow(vehicleX, vehicleY, width, height);
            
            // Phares/feux si nécessaire
            drawVehicleLights(vehicleX, vehicleY, width, height, vs);
        }
    }
    
    private void drawRealisticVehicle(double x, double y, double width, double height, VehicleState vs) {
        // Corps du véhicule
        gc.setFill(vs.vehicleColor);
        gc.fillRoundRect(x - width/2, y - height/2, width, height, 5, 5);
        
        // Dégradé pour effet 3D
        gc.setFill(vs.vehicleColor.brighter());
        gc.fillRoundRect(x - width/2 + 2, y - height/2 + 2, width - 4, height/3, 3, 3);
        
        // Vitres
        gc.setFill(Color.rgb(200, 220, 240, 0.6));
        gc.fillRoundRect(x - width/2 + 5, y - height/2 + 5, width - 10, height/3, 2, 2);
        
        // Roues
        gc.setFill(Color.rgb(40, 40, 40));
        double wheelY = y + height/2 - 5;
        gc.fillOval(x - width/3 - 8, wheelY - 5, 10, 10);
        gc.fillOval(x + width/3 - 2, wheelY - 5, 10, 10);
        
        // Info vitesse
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 9));
        String speedText = String.format("%.0f", vs.currentSpeed) + " km/h";
        gc.fillText(speedText, x - 20, y - height/2 - 10);
        
        // Indicateur de freinage
        if (vs.isBraking) {
            gc.setFill(Color.RED);
            gc.fillOval(x + width/2 - 5, y - 3, 6, 6);
        }
    }
    
    private void drawVehicleShadow(double x, double y, double width, double height) {
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(x - width/2, y + height/2 - 5, width, 10);
    }
    
    private void drawVehicleLights(double x, double y, double width, double height, VehicleState vs) {
        // Phares avant
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - width/2 + 5, y - height/4, 4, 4);
        
        // Feux arrière
        gc.setFill(Color.RED);
        gc.fillOval(x + width/2 - 9, y - height/4, 4, 4);
        
        // Clignotants aléatoires
        if (random.nextDouble() < 0.005) {
            gc.setFill(Color.ORANGE);
            gc.fillOval(x + width/2 - 9, y - height/2 + 5, 3, 6);
        }
    }
    
    private void drawHUD() {
        // Panneau d'information semi-transparent
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRoundRect(10, 10, 300, 140, 10, 10);
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("📊 TABLEAU DE BORD", 20, 30);
        
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Route: " + routeSelectionnee.getNom(), 20, 50);
        gc.fillText("Véhicules: " + vehicleStates.size(), 20, 70);
        
        // Statistiques avancées
        double avgSpeed = vehicleStates.stream()
            .mapToDouble(vs -> vs.currentSpeed)
            .average()
            .orElse(0);
        gc.fillText("Vitesse moyenne: " + String.format("%.1f", avgSpeed) + " km/h", 20, 90);
        
        long vehiclesMoving = vehicleStates.stream()
            .filter(vs -> vs.currentSpeed > 5)
            .count();
        gc.fillText("Véhicules en mouvement: " + vehiclesMoving, 20, 110);
        
        // État simulation
        String state = isPaused ? "⏸️ PAUSE" : (simulationSpeed > 1 ? "⏩ ACCÉLÉRÉ" : "▶️ EN COURS");
        gc.fillText("État: " + state + " (x" + String.format("%.1f", simulationSpeed) + ")", 20, 130);
        
        // Mini-carte de la route
        drawMiniMap();
    }
    
    private void drawMiniMap() {
        double mapX = simulationCanvas.getWidth() - 150;
        double mapY = 20;
        double mapSize = 120;
        
        // Fond carte
        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRoundRect(mapX, mapY, mapSize, mapSize, 5, 5);
        
        // Route miniature
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        double routeStartX = mapX + 10;
        double routeWidth = mapSize - 20;
        
        // Position des véhicules sur la mini-carte
        for (VehicleState vs : vehicleStates) {
            double posX = routeStartX + vs.currentPosition * routeWidth;
            double posY = mapY + mapSize/2;
            
            gc.setFill(vs.vehicleColor);
            gc.fillOval(posX - 2, posY - 2, 4, 4);
        }
        
        // Titre mini-carte
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("🗺️ VUE D'ENSEMBLE", mapX + 5, mapY + 15);
    }
    
    private void drawSpecialEffects() {
        // Soleil
        double sunX = simulationCanvas.getWidth() - 100;
        double sunY = 100;
        gc.setFill(Color.rgb(255, 255, 200, 0.8));
        gc.fillOval(sunX - 25, sunY - 25, 50, 50);
        
        // Rayons de soleil
        gc.setStroke(Color.rgb(255, 255, 200, 0.4));
        gc.setLineWidth(2);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double rayLength = 40;
            double endX = sunX + Math.cos(angle) * rayLength;
            double endY = sunY + Math.sin(angle) * rayLength;
            gc.strokeLine(sunX, sunY, endX, endY);
        }
        
        // Oiseaux (effet décoratif)
        drawBirds();
    }
    
    private void drawBirds() {
        gc.setStroke(Color.rgb(255, 255, 255, 0.6));
        gc.setLineWidth(1);
        
        for (int i = 0; i < 3; i++) {
            double birdX = 50 + i * 100 + (simulationTime / 5000.0) % 300;
            double birdY = 80 + i * 20;
            
            // Dessiner un oiseau simple (V shape)
            gc.strokeLine(birdX, birdY, birdX - 5, birdY + 5);
            gc.strokeLine(birdX, birdY, birdX + 5, birdY + 5);
        }
    }
    
    private Color generateRandomColor() {
        int r = 50 + random.nextInt(150); // Éviter les couleurs trop claires ou trop foncées
        int g = 50 + random.nextInt(150);
        int b = 50 + random.nextInt(150);
        return Color.rgb(r, g, b);
    }
    
    private double calculateRouteLength() {
        // Calcul simplifié de la longueur de route
        List<Route.Point> points = routeSelectionnee.getPoints();
        if (points.size() < 2) return 10.0; // Longueur par défaut
        
        double total = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Route.Point p1 = points.get(i);
            Route.Point p2 = points.get(i + 1);
            double latDiff = p2.getLatitude() - p1.getLatitude();
            double lngDiff = p2.getLongitude() - p1.getLongitude();
            total += Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
        }
        return Math.round(total * 110 * 10) / 10.0; // Conversion approximative en km
    }
    
    private double calculateAverageSpeed() {
        return routeSelectionnee.getVehicules().stream()
            .mapToDouble(Vehicle::getVitesse)
            .average()
            .orElse(0);
    }

    public void setOnBackRequest(Runnable onBackRequest) {
        this.onBackRequest = onBackRequest;
    }

    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        simulationManager.arreterSimulation();
        System.out.println("🛑 SimulationView optimisée arrêtée");
    }
}