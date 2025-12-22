package com.example.demo.controller;

import com.example.demo.model.Route;
import com.example.demo.view.RouteDetailsView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RouteDetailsManager {
    private static RouteDetailsManager instance;

    public static synchronized RouteDetailsManager getInstance() {
        if (instance == null) {
            instance = new RouteDetailsManager();
        }
        return instance;
    }

    private RouteDetailsManager() {
        System.out.println("🔄 RouteDetailsManager initialisé");
    }

    // 🔥 MÉTHODE GARANTIE POUR OUVRIR LES DÉTAILS
    public void ouvrirDetailsRoute(Route route) {
        System.out.println("🚀🚀🚀 OUVRIR DETAILS ROUTE APPELÉ 🚀🚀🚀");
        System.out.println("📊 Route: " + route.getNom());

        Platform.runLater(() -> {
            try {
                System.out.println("🎬 Création de RouteDetailsView...");

                // VERSION 1: Simple et efficace
                RouteDetailsView detailsView = new RouteDetailsView(route);

                System.out.println("🎨 Création de la scène...");
                Scene scene = new Scene(detailsView, 1000, 700);

                System.out.println("🏗️ Création du stage...");
                Stage stage = new Stage();
                stage.setTitle("Détails Route: " + route.getNom());
                stage.setScene(scene);

                // FORCER l'affichage
                stage.setAlwaysOnTop(true); // Pour être sûr qu'elle apparaît
                stage.show();
                stage.toFront(); // Mettre au premier plan
                stage.setAlwaysOnTop(false); // Revenir à la normale

                System.out.println("✅✅✅ FENÊTRE DES DÉTAILS OUVERTE AVEC SUCCÈS !");
                System.out.println("🔍 Vérifiez si la fenêtre est derrière d'autres fenêtres");

                // Log supplémentaire
                Platform.runLater(() -> {
                    System.out.println("📝 Fenêtre visible: " + stage.isShowing());
                    System.out.println("📝 Fenêtre focused: " + stage.isFocused());
                });

            } catch (Exception e) {
                System.err.println("❌❌❌ ERREUR CRITIQUE: " + e.getMessage());
                e.printStackTrace();

                // Fallback: Message d'erreur
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible d'ouvrir les détails");
                alert.setContentText("Erreur: " + e.getMessage() + "\n\nRoute: " + route.getNom());
                alert.showAndWait();
            }
        });
    }
}
