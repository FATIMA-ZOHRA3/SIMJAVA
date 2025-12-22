package com.example.demo.view;

import com.example.demo.controller.SimulationManager;
import com.example.demo.model.Route;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;

public class RouteSelectionView extends VBox {
    
    private SimulationManager simulationManager;
    private Runnable onRouteSelected;
    
    public RouteSelectionView(SimulationManager simulationManager, Runnable onRouteSelected) {
        this.simulationManager = simulationManager;
        this.onRouteSelected = onRouteSelected;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: linear-gradient(to bottom, #667eea, #764ba2);");
        
        // Titre
        Label titleLabel = new Label("🚦 SÉLECTION DE ROUTE");
        titleLabel.setFont(Font.font("Arial", 28));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        // Description
        Label descLabel = new Label("Choisissez une route pour démarrer la simulation de trafic");
        descLabel.setFont(Font.font("Arial", 14));
        descLabel.setTextFill(Color.WHITE);
        
        // Liste des routes
        VBox routesContainer = new VBox(15);
        routesContainer.setAlignment(Pos.CENTER);
        routesContainer.setPadding(new Insets(20));
        routesContainer.setMaxWidth(500);
        
        List<Route> routes = simulationManager.getRoutesDisponibles();
        
        if (routes.isEmpty()) {
            Label noRoutesLabel = new Label("Aucune route disponible");
            noRoutesLabel.setTextFill(Color.WHITE);
            routesContainer.getChildren().add(noRoutesLabel);
        } else {
            for (Route route : routes) {
                routesContainer.getChildren().add(createRouteCard(route));
            }
        }
        
        getChildren().addAll(titleLabel, descLabel, routesContainer);
    }
    
    private VBox createRouteCard(Route route) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 15;");
        card.setMaxWidth(400);
        
        // Nom de la route
        Label nameLabel = new Label("🛣️ " + route.getNom());
        nameLabel.setFont(Font.font("Arial", 18));
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        // Informations
        Label infoLabel = new Label(
            "• Véhicules: " + route.getVehicules().size() + "\n" +
            "• Points: " + route.getPoints().size() + "\n" +
            "• Longueur: " + String.format("%.1f", calculerDistance(route)) + " km"
        );
        infoLabel.setFont(Font.font("Arial", 12));
        
        // Bouton sélection
        Button selectButton = new Button("🎮 Démarrer la simulation");
        selectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        selectButton.setOnAction(e -> {
            simulationManager.demarrerSimulation(route.getId());
            if (onRouteSelected != null) {
                onRouteSelected.run();
            }
        });
        
        card.getChildren().addAll(nameLabel, infoLabel, selectButton);
        return card;
    }
    
    private double calculerDistance(Route route) {
        // Calcul de distance simplifié (Haversine)
        if (route.getPoints().size() < 2) return 0;
        
        Route.Point p1 = route.getPoints().get(0);
        Route.Point p2 = route.getPoints().get(route.getPoints().size() - 1);
        
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());
        
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371; // Rayon de la Terre en km
        
        return c * r;
    }
}