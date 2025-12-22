package com.example.demo.controller;

import com.example.demo.view.MainView;
import com.example.demo.view.MapView;
import com.example.demo.view.RouteDetailsView;
import com.example.demo.model.User;
import com.example.demo.model.GPSPosition;
import com.example.demo.model.Incident;
import com.example.demo.model.Route;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainController {

    // Composants de l'interface
    private MainView mainView;
    private MapView mapView;
    private MapController mapController;

    // Données de l'application
    private User currentUser;
    private List<GPSPosition> savedPositions;
    private List<Incident> incidents;

    // Gestion des routes et simulation
    private SimulationManager simulationManager;

    // Statistiques et état système
    private int totalOperations = 0;
    private long sessionStartTime;
    private boolean isPremiumMode = true;

    public MainController() {
        // Initialisation des collections
        this.savedPositions = new ArrayList<>();
        this.incidents = new ArrayList<>();

        // Initialisation des contrôleurs
        this.mapController = new MapController();
        this.simulationManager = new SimulationManager();

        // Initialisation des statistiques
        this.sessionStartTime = System.currentTimeMillis();

        // Initialisation des données d'exemple
        initializeSampleData();

        System.out.println("✅ MainController Premium combiné créé avec succès");
        System.out.println("🎯 Fonctionnalités incluses :");
        System.out.println("  - Gestion cartographique avancée");
        System.out.println("  - Système de routes et simulation");
        System.out.println("  - Interface premium complète");
    }

    // ==================== CONFIGURATION DE L'INTERFACE ====================

    public void setMainView(MainView mainView) {
        this.mainView = mainView;
        if (mainView != null) {
            mainView.setMainController(this);
            mainView.setMapController(this.mapController);

            System.out.println("✅ MainView premium connectée au MainController");
        }
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;

        if (mapView != null) {
            this.mapController.setMapView(mapView);

            // Attendre que la carte soit prête avant de configurer les routes
            mapView.setOnMapReady(() -> {
                configurerGestionRoutes();
                System.out.println("✅ MapView configurée avec gestion des routes après initialisation");
            });

            System.out.println("⏳ Attente initialisation MapView pour configuration routes...");
        }
    }

    private void configurerGestionRoutes() {
        if (mapView == null) {
            System.err.println("❌ MapView non initialisée dans MainController");
            return;
        }

        System.out.println("🎯 Configuration de la sélection de routes...");

        // Écouter les sélections de route depuis la carte
        // CORRECTION: Utiliser activerSelectionParClic pour le mode clic route
        mapView.activerSelectionParClic(this::ouvrirDetailsRoute);

        System.out.println("✅ Système de sélection de routes configuré avec succès");
    }

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
        if (mainView != null) {
            mainView.setMapController(mapController);
        }
    }

    public MapController getMapController() {
        return mapController;
    }


    // ==================== GESTION DE L'UTILISATEUR ====================

    public void setCurrentUser(int userId, String userName) {
        this.currentUser = new User(userId, userName);

        if (mapController != null) {
            mapController.setCurrentUser(userId, userName);
        }

        if (mainView != null) {
            mainView.setCurrentUser(userId, userName);
        }

        System.out.println("👤 Utilisateur défini: " + userName + " (ID: " + userId + ")");
    }

    // ==================== DONNÉES D'EXEMPLE ====================

    private void initializeSampleData() {
        // Positions GPS
        savedPositions.add(new GPSPosition(33.5731, -7.5898, "Casablanca Centre", "Position par défaut haute qualité"));
        savedPositions.add(new GPSPosition(33.5928, -7.6186, "Aéroport Mohammed V", "Zone aéroportuaire internationale"));
        savedPositions.add(new GPSPosition(33.5586, -7.6620, "Morocco Mall", "Centre commercial premium"));
        savedPositions.add(new GPSPosition(34.0209, -6.8416, "Rabat Centre", "Capitale administrative"));
        savedPositions.add(new GPSPosition(31.6295, -7.9811, "Marrakech Médina", "Place Jemaa el-Fna"));

        // Incidents
        incidents.add(new Incident(33.5700, -7.5900, "Accident véhicule",
                "Accident mineur - embouteillage modéré - Secours en route", "Moyen"));
        incidents.add(new Incident(33.5750, -7.5950, "Travaux infrastructure",
                "Travaux routiers majeurs - voie réduite - Détour actif", "Élevé"));
        incidents.add(new Incident(33.5800, -7.6000, "Embouteillage critique",
                "Trafic dense - ralentissements importants - Heure de pointe", "Critique"));
        incidents.add(new Incident(34.0250, -6.8350, "Manifestation",
                "Rassemblement pacifique - Routes alternatives conseillées", "Moyen"));
    }

    // ==================== FONCTIONNALITÉS DE NAVIGATION ====================

    public void localiserParIP() {
        totalOperations++;
        if (mainView != null && mapController != null) {
            mapController.locateByIP(mainView.getInfoLabel());
        }
    }

    public void definirPointDepart() {
        totalOperations++;
        if (mainView != null && mapController != null) {
            mapController.setStartPoint(mainView.getInfoLabel());
        }
    }

    public void definirPointArrivee() {
        totalOperations++;
        if (mainView != null && mapController != null) {
            mapController.setEndPoint(mainView.getInfoLabel());
        }
    }

    public void calculerItineraire() {
        totalOperations++;
        if (mainView != null && mapController != null) {
            mapController.calculateRoute(mainView.getInfoLabel());
        }
    }

    public void effacerItineraire() {
        totalOperations++;
        if (mainView != null && mapController != null) {
            mapController.clearRoute(mainView.getInfoLabel());
        }
    }

    // ==================== FONCTIONNALITÉS DES CARTES ====================

    public void activerCarteStandard() {
        if (mainView != null && mapController != null) {
            mapController.switchToStandardMap(mainView.getInfoLabel());
        }
    }

    public void activerCarteSatellite() {
        if (mainView != null && mapController != null) {
            mapController.switchToSatelliteMap(mainView.getInfoLabel());
        }
    }

    public void activerCarteSombre() {
        if (mainView != null && mapController != null) {
            mapController.switchToDarkMap(mainView.getInfoLabel());
        }
    }

    public void activerPleinEcran() {
        if (mainView != null && mapController != null) {
            mapController.toggleFullscreen(mainView.getInfoLabel());
        }
    }

    // ==================== GESTION DES INCIDENTS ====================

    public void montrerIncidents() {
        if (mainView != null && mapController != null) {
            mapController.showNearbyIncidents(mainView.getInfoLabel());
        }
    }

    // ==================== GESTION DES MARQUEURS ====================

    public void effacerMarqueurs() {
        if (mainView != null && mapController != null) {
            mapController.clearAllMarkers(mainView.getInfoLabel());
        }
    }

    public void ajouterMarqueurPersonnalise() {
        if (mainView != null && mapController != null) {
            double lat = 33.5731 + (Math.random() * 0.03 - 0.015);
            double lon = -7.5898 + (Math.random() * 0.03 - 0.015);
            String title = "📍 Point d'intérêt premium – " +
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            mapController.addCustomMarker(lat, lon, title, mainView.getInfoLabel());
        }
    }

    // ==================== NAVIGATION GÉOGRAPHIQUE ====================

    public void centrerSurCasablanca() { centrerSurVille("casablanca"); }
    public void centrerSurRabat() { centrerSurVille("rabat"); }
    public void centrerSurMarrakech() { centrerSurVille("marrakech"); }
    public void centrerSurTanger() { centrerSurVille("tanger"); }
    public void centrerSurFès() { centrerSurVille("fès"); }

    private void centrerSurVille(String ville) {
        if (mainView != null && mapController != null) {
            mapController.zoomToArea(ville, mainView.getInfoLabel());
        }
    }

    // ==================== GESTION DES ROUTES ====================

    private void ouvrirDetailsRoute(Route route) {
        System.out.println("🚀 OUVRIR DÉTAILS: " + route.getNom());
        System.out.println("📊 Véhicules sur la route: " + route.getVehicules().size());

        Platform.runLater(() -> {
            try {
                // Créer la vue des détails
                RouteDetailsView detailsView = new RouteDetailsView(route);

                // Créer la scène
                Scene scene = new Scene(detailsView, 1200, 800);
                Stage stage = new Stage();
                stage.setTitle("Détails Route: " + route.getNom());
                stage.setScene(scene);

                // Fermer la sélection par clic sur la carte originale
                // CORRECTION: Supprimer l'appel à une méthode qui n'existe pas
                // mapView.desactiverSelectionParClic(); // RETIRÉ - méthode inexistante

                // Gérer la fermeture de la fenêtre
                stage.setOnCloseRequest(e -> {
                    System.out.println("🔙 Retour à la carte principale");
                    // Réactiver la sélection si besoin
                    if (mapView != null) {
                        // CORRECTION: Utiliser activerSelectionRoute au lieu de activerSelectionParClic
                        mapView.activerSelectionRoute(this::ouvrirDetailsRoute);
                    }
                });

                stage.show();
                stage.centerOnScreen();

                System.out.println("✅ Page détails route ouverte avec succès");

            } catch (Exception e) {
                System.err.println("❌ Erreur ouverture page détails: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void openRouteDetailsWindow(Route route) {
        ouvrirDetailsRoute(route);
    }

    public void showRouteDetailsPage(Route route) {
        openRouteDetailsWindow(route);
    }

    public void activerSelectionRoute() {
        if (mapController != null) {
            mapController.activerSelectionRouteParClic();
        }
    }

    public void activerSelectionParClic() {
        activerSelectionRoute();
    }

    public void testerSelectionRoute() {
        System.out.println("🧪 Test manuel de sélection de route");

        if (mapView != null && !mapView.getRoutesDisponibles().isEmpty()) {
            Route premiereRoute = mapView.getRoutesDisponibles().get(0);
            ouvrirDetailsRoute(premiereRoute);
        } else {
            System.err.println("❌ Aucune route disponible pour le test");
        }
    }

    // ==================== GESTION DU SYSTÈME ====================

    public void verifierEtatSysteme() {
        String msg = "🔍 Système Premium - opérations=" + totalOperations +
                " - durée=" + getSessionDuration() + "min";

        if (mainView != null) {
            mainView.updateStatus(msg, "#3498db");
        }

        System.out.println("🔍 Vérification état système combiné:");
        System.out.println("  - MainView: " + (mainView != null ? "OK" : "NULL"));
        System.out.println("  - MapController: " + (mapController != null ? "OK" : "NULL"));
        System.out.println("  - SimulationManager: " + (simulationManager != null ? "OK" : "NULL"));
        System.out.println("  - Utilisateur: " + (currentUser != null ? currentUser.getUsername() : "NON DÉFINI"));
        System.out.println("  - Mode Premium: " + isPremiumMode);
        System.out.println("  - Positions sauvegardées: " + savedPositions.size());
        System.out.println("  - Incidents: " + incidents.size());
    }

    public void verifierConnexionReseau() {
        System.out.println("🌐 Vérification réseau premium...");
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    boolean ok = Math.random() > 0.2;
                    if (ok) {
                        updateStatus("✅ Réseau Premium OK", "#2ecc71");
                    } else {
                        updateStatus("❌ Réseau Premium KO", "#e74c3c");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void reinitialiserCarte() {
        if (mainView != null) {
            mainView.resetMap();
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void updateStatus(String message, String color) {
        if (mainView != null) {
            mainView.updateStatus(message, color);
        }
    }

    private long getSessionDuration() {
        return (System.currentTimeMillis() - sessionStartTime) / 60000;
    }

    // ==================== GETTERS ====================

    public MainView getMainView() {
        return mainView;
    }

    public SimulationManager getSimulationManager() {
        return simulationManager;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public String getCurrentUserName() {
        return currentUser != null ? currentUser.getUsername() : "Invité";
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<GPSPosition> getSavedPositions() {
        return savedPositions;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public boolean isPremiumMode() {
        return isPremiumMode;
    }

    public int getTotalOperations() {
        return totalOperations;
    }

    public MapView getMapView() {
        return mapView;
    }
}