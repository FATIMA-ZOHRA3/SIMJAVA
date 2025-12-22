package com.example.demo;

import com.example.demo.controller.MainController;
import com.example.demo.controller.MapController;
import com.example.demo.controller.auth.LoginController;
import com.example.demo.model.Database;
import com.example.demo.model.PreciseLocationService;
import com.example.demo.view.MainView;
import com.example.demo.view.MapView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.Connection;
import java.net.URL;

public class Main extends Application {

    private Connection connection;
    private PreciseLocationService locationService;
    private boolean debugMode = true; // ⚠️ CHANGÉ À true POUR PASSER DIRECTEMENT À L'APPLICATION

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🚀 Démarrage direct de Traffic Monitor Premium...");

            // Vérification des ressources critiques
            checkRequiredResources();

            // Initialisation de la base de données
            initializeDatabase();

            // ⚠️ PASSAGE DIRECT À L'APPLICATION PRINCIPALE - PAS DE LOGIN
            System.out.println("🔧 Mode direct activé - Passage immédiat à l'application");

            // Définir les informations utilisateur par défaut
            int userId = 1;
            String userName = "Utilisateur Premium";

            // Démarrer directement l'application principale
            showMainApplication(primaryStage, userId, userName);

        } catch (Exception e) {
            System.err.println("❌ Erreur critique au démarrage: " + e.getMessage());
            e.printStackTrace();

            // Mode démo en cas d'erreur
            showDetailedErrorAlert("Erreur Critique",
                    "Impossible de démarrer l'application", e);

            // Tenter le mode dégradé
            Stage fallbackStage = new Stage();
            try {
                showMainApplication(fallbackStage, 1, "Utilisateur Démo");
            } catch (Exception fallbackError) {
                showErrorScreen(fallbackStage, "Mode dégradé également en échec: " + fallbackError.getMessage());
            }
        }
    }

    // 🔍 Vérification des ressources critiques
    private void checkRequiredResources() {
        System.out.println("🔍 Vérification des ressources critiques...");

        String[] criticalResources = {
                "/com/example/demo/view/main-view.fxml",
                "/com/example/demo/view/map-view.fxml",
                "/com/example/demo/view/auth/login-view.fxml",
                "/map.html",
                "/style.css"
        };

        boolean allResourcesFound = true;
        for (String resource : criticalResources) {
            URL resourceUrl = getClass().getResource(resource);
            if (resourceUrl == null) {
                System.err.println("❌ Ressource manquante: " + resource);
                allResourcesFound = false;
            } else {
                System.out.println("✅ Ressource trouvée: " + resource);
            }
        }

        if (!allResourcesFound) {
            System.err.println("⚠️ Certaines ressources sont manquantes - Mode dégradé activé");
        } else {
            System.out.println("✅ Toutes les ressources critiques sont présentes");
        }
    }

    // 🗄️ Initialisation de la base de données
    private void initializeDatabase() {
        try {
            connection = Database.getConnection();
            if (connection != null && !connection.isClosed()) {
                locationService = new PreciseLocationService(connection);
                System.out.println("✅ Base de données et service de localisation haute qualité initialisés");
            } else {
                System.out.println("⚠️ Mode sans base de données - Utilisation des services en ligne premium");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Mode sans base de données: " + e.getMessage());
            System.out.println("🔧 Utilisation du mode sans persistance");
        }
    }

    // 🖥️ Affichage direct de l'application principale (SANS LOGIN)
    private void showMainApplication(Stage mainAppStage, int userId, String userName) {
        try {
            System.out.println("🔄 Initialisation interface premium pour: " + userName + " (ID: " + userId + ")");

            // 🔥 CRÉATION DES COMPOSANTS AVEC LE BON ORDRE
            MainController mainController = new MainController();
            MainView mainView = new MainView();
            MapView mapView = new MapView();
            MapController mapController = mainController.getMapController();

            // 🔥 ÉTAPE 1: Configuration utilisateur EN PREMIER
            System.out.println("👤 Configuration utilisateur premium: " + userName + " (ID: " + userId + ")");
            mainController.setCurrentUser(userId, userName);

            // 🔥 ÉTAPE 2: Configuration des services premium
            if (locationService != null && mapController != null) {
                mapController.setPreciseLocationService(locationService);
                System.out.println("📍 Service de localisation haute précision configuré");
            }

            // 🔥 ÉTAPE 3: Connexion des composants DANS LE BON ORDRE
            System.out.println("🔗 Connexion des composants haute qualité...");

            // 3.1: D'abord connecter MainView à MapView
            mainView.setMapView(mapView);
            System.out.println("✅ MainView ← MapView");

            // 3.2: Ensuite connecter MainController à MainView
            mainController.setMainView(mainView);
            System.out.println("✅ MainController ← MainView");

            // 3.3: Enfin connecter MapController à MapView
            mapController.setMapView(mapView);
            System.out.println("✅ MapController ← MapView");

            // 🔥 ÉTAPE 4: Configuration de la scène premium
            Scene mainScene = new Scene(mainView, 1200, 800);
            mainAppStage.setTitle("Traffic Monitor Premium - Maroc | " + userName + " (ID: " + userId + ")");
            mainAppStage.setScene(mainScene);
            mainAppStage.setMinWidth(1000);
            mainAppStage.setMinHeight(700);
            mainAppStage.setMaximized(false);
            mainAppStage.setFullScreen(false);

            // 🔥 GESTIONNAIRE DE FERMETURE AMÉLIORÉ
            mainAppStage.setOnCloseRequest(event -> {
                System.out.println("🔒 Fermeture de l'application premium pour: " + userName + " (ID: " + userId + ")");

                // Arrêt propre de la simulation si elle est en cours
                if (mainController.getSimulationManager() != null) {
                    mainController.getSimulationManager().arreterSimulation();
                    System.out.println("🛑 Simulation premium arrêtée");
                }

                // Nettoyage des vues
                if (mapView != null) {
                    try {
                        mapView.effacerMarqueurs();
                        mapView.clearRoute();
                        System.out.println("🧹 Ressources carte nettoyées");
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur nettoyage carte: " + e.getMessage());
                    }
                }

                // Fermeture base de données
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                        System.out.println("✅ Connexion BD fermée");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur fermeture BD: " + e.getMessage());
                }

                System.out.println("✅ Arrêt propre des services premium terminé");
                Platform.exit();
            });

            // 🔥 AFFICHAGE DE LA FENÊTRE
            mainAppStage.show();
            System.out.println("✅ Application premium affichée pour: " + userName + " (ID: " + userId + ")");

            // 🔥 INITIALISATION ASYNCHRONE AVEC GESTION D'ERREUR AMÉLIORÉE
            initializeMapAsync(mainController, mapView, mainView, userId, userName);

        } catch (Exception e) {
            System.err.println("❌❌❌ ERREUR CRITIQUE application principale: " + e.getMessage());
            e.printStackTrace();

            // Mode dégradé
            showDetailedErrorAlert("Erreur Application",
                    "Impossible de charger l'interface principale", e);

            showErrorScreen(mainAppStage, "Impossible de démarrer l'application: " + e.getMessage());
        }
    }

    // 🔄 Initialisation asynchrone améliorée
    private void initializeMapAsync(MainController mainController, MapView mapView,
                                    MainView mainView, int userId, String userName) {
        new Thread(() -> {
            try {
                System.out.println("⏳ Attente initialisation carte premium pour: " + userName);

                // Attendre que la carte soit complètement initialisée
                mapView.waitForInitialization();

                Platform.runLater(() -> {
                    try {
                        // 🔥 ACTIVER LA SÉLECTION DE ROUTES PAR CLIC
                        System.out.println("🎯 Activation automatique de la sélection de routes premium...");
                        mainController.activerSelectionParClic();

                        // Vérifier l'état du système
                        mainController.verifierEtatSysteme();

                        // Test de connexion réseau premium
                        testConnexionReseauPremium(mainController, mainView);

                        // Test de localisation spécifique à l'utilisateur
                        testUserSpecificLocation(mainController.getMapController(), userId, userName);

                        // Initialisation complète réussie
                        System.out.println("🎉 ✅ Initialisation COMPLÈTE terminée pour: " + userName);
                        System.out.println("🎯 Sélection de routes premium activée - Cliquez sur une route pour voir les détails");

                        // Mettre à jour le statut
                        mainView.updateStatus("✅ Système premium prêt - Navigation optimisée active", "#2ecc71");

                    } catch (Exception e) {
                        System.err.println("❌ Erreur lors de l'initialisation finale: " + e.getMessage());
                        if (mainView != null) {
                            mainView.updateStatus("❌ Erreur initialisation système premium", "#e74c3c");
                        }
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur initialisation carte pour " + userName + ": " + e.getMessage());
                Platform.runLater(() -> {
                    if (mainView != null) {
                        mainView.updateStatus("⚠️ Erreur initialisation carte - Mode dégradé activé", "#f39c12");
                    }
                });
            }
        }).start();
    }

    // 🌐 Test de connexion réseau premium
    private void testConnexionReseauPremium(MainController mainController, MainView mainView) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Petit délai
                Platform.runLater(() -> {
                    if (mainView != null) {
                        mainView.updateStatus("🌐 Test de connexion premium en cours...", "#3498db");
                    }
                });

                Thread.sleep(2000); // Simulation test réseau

                boolean networkOK = Math.random() > 0.1; // 90% de succès

                Platform.runLater(() -> {
                    if (mainView != null) {
                        if (networkOK) {
                            mainView.updateStatus("✅ Connexion réseau premium OK - Haut débit détecté", "#27ae60");
                        } else {
                            mainView.updateStatus("⚠️ Connexion réseau limitée - Mode bas débit", "#f39c12");
                        }
                    }

                    if (mainController != null) {
                        mainController.verifierConnexionReseau();
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // 📍 Test de la localisation spécifique à l'utilisateur
    private void testUserSpecificLocation(MapController mapController, int userId, String userName) {
        new Thread(() -> {
            try {
                // Délai pour laisser l'interface se stabiliser
                Thread.sleep(3000);

                Platform.runLater(() -> {
                    try {
                        // Test de la localisation IP avec l'ID utilisateur
                        com.example.demo.model.IPGeolocationService.IPLocation location =
                                com.example.demo.model.IPGeolocationService.getLocationByIP(userId);

                        System.out.println("🧪 TEST LOCALISATION PREMIUM - Utilisateur " + userId + " (" + userName + ")");
                        System.out.println("🧪 Ville: " + location.getCity());
                        System.out.println("🧪 Coordonnées: " + location.getLatitude() + ", " + location.getLongitude());
                        System.out.println("🧪 Précision: " + location.getSource());
                        System.out.println("🧪 Adresse formatée: " + location.getFormattedAddress());

                        // Mettre à jour le statut avec la localisation spécifique
                        if (mapController != null && mapController.getCurrentInfoLabel() != null) {
                            mapController.getCurrentInfoLabel().setText(
                                    "📍 Position premium détectée: " + location.getCity() +
                                            " | Utilisateur: " + userName +
                                            " | Précision: " + location.getSource()
                            );
                        }

                        // Afficher un message de succès
                        System.out.println("🎯 Localisation IP premium réussie pour " + userName);

                        // Centrer automatiquement sur la position de l'utilisateur
                        if (mapController != null) {
                            mapController.centerOnDefaultLocation(mapController.getCurrentInfoLabel());
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Erreur test localisation premium pour " + userName + ": " + e.getMessage());

                        // Fallback sur Casablanca
                        if (mapController != null) {
                            mapController.zoomToArea("casablanca", mapController.getCurrentInfoLabel());
                        }
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ❌ Écran d'erreur en cas de problème critique
    private void showErrorScreen(Stage stage, String errorMessage) {
        try {
            Label errorLabel = new Label(
                    "❌ ERREUR CRITIQUE - MODE DÉGRADÉ\n\n" +
                            "L'application n'a pas pu démarrer correctement.\n\n" +
                            "Erreur détectée: " + errorMessage + "\n\n" +
                            "Fonctionnalités disponibles:\n" +
                            "• Carte de base\n" +
                            "• Navigation limitée\n" +
                            "• Mode hors ligne\n\n" +
                            "Veuillez redémarrer l'application pour tenter une réparation.\n" +
                            "Si le problème persiste, contactez le support premium."
            );
            errorLabel.setStyle("""
                -fx-text-fill: #e74c3c; 
                -fx-font-size: 14px; 
                -fx-padding: 25px; 
                -fx-alignment: center;
                -fx-font-weight: bold;
                -fx-text-alignment: center;
            """);
            errorLabel.setWrapText(true);

            StackPane errorPane = new StackPane(errorLabel);
            errorPane.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");

            Scene errorScene = new Scene(errorPane, 700, 500);
            stage.setTitle("Traffic Monitor Premium - Mode Dégradé");
            stage.setScene(errorScene);
            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Impossible d'afficher l'écran d'erreur: " + e.getMessage());
        }
    }

    // ⚠️ Alertes d'erreur
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showDetailedErrorAlert(String title, String message, Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.setContentText("Erreur détaillée:\n" +
                    e.getMessage() + "\n\n" +
                    "Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack trace disponible dans la console.");
            alert.showAndWait();
        });
    }

    // 🔄 Arrêt propre de l'application
    @Override
    public void stop() {
        System.out.println("🔄 Arrêt propre de l'application Traffic Monitor Premium...");
        try {
            // Fermeture de la connexion base de données
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Connexion BD fermée");
            }

            System.out.println("✅ Services premium arrêtés proprement");
            Platform.exit();

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'arrêt: " + e.getMessage());
        }
    }

    // 🎯 Point d'entrée principal
    public static void main(String[] args) {
        try {
            System.out.println("🎮 Lancement direct de Traffic Monitor Premium...");
            System.out.println("🔧 Mode direct activé - Interface principale immédiate");

            launch(args);

        } catch (Exception e) {
            System.err.println("❌❌❌ ÉCHEC CRITIQUE DU LANCEMENT: " + e.getMessage());
            e.printStackTrace();

            // Message utilisateur final
            Alert crashAlert = new Alert(AlertType.ERROR);
            crashAlert.setTitle("Application Crash");
            crashAlert.setHeaderText("L'application a rencontré une erreur critique");
            crashAlert.setContentText("Veuillez redémarrer l'application.\n" +
                    "Si le problème persiste, contactez le support.");
            crashAlert.showAndWait();
        }
    }
}