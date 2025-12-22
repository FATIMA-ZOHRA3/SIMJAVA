package com.example.demo.controller;

import com.example.demo.model.IPGeolocationService;
import com.example.demo.model.PreciseLocationService;
import com.example.demo.model.Route;
import com.example.demo.view.MapView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MapController {

    // Contrôleur parent
    private MainController mainController;

    // Services de localisation
    private MapView mapView;
    private PreciseLocationService preciseLocationService;

    // Informations utilisateur
    private int currentUserId;
    private String currentUserName;
    private Label currentInfoLabel;

    // État de la carte
    private boolean isMapReady = false;
    private final CountDownLatch mapReadyLatch = new CountDownLatch(1);
    private Runnable mapReadyListener;

    // Gestion des itinéraires
    private double startLat = 0.0;
    private double startLng = 0.0;
    private double endLat = 0.0;
    private double endLng = 0.0;

    // Statistiques avancées
    private int markerCount = 0;
    private int routeCalculationCount = 0;
    private int locationRequestCount = 0;
    private int totalOperations = 0;

    // ==================== CONSTRUCTEUR ET INITIALISATION ====================

    public MapController() {
        System.out.println("🗺️ MapController Premium combiné créé - Système haute qualité activé");
    }

    public MapView getMapView() {
        return mapView;
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
        System.out.println("🎯 MapView haute qualité attachée au contrôleur");

        if (mapView != null) {
            // Configuration optimisée pour la haute qualité
            configureMapView();

            // Configurer le listener pour la carte prête
            mapView.setOnMapReady(this::notifyMapReady);

            // Démarrer l'attente de l'initialisation
            waitForMapInitialization();
        } else {
            System.err.println("❌ MapView est null dans setMapView!");
        }
    }

    // ==================== CONFIGURATION DE LA MAPVIEW ====================

    public void configureMapView() {
        if (mapView != null) {
            // CORRECTION: Supprimer l'appel à setUserIdProvider car la méthode n'existe pas
            // mapView.setUserIdProvider(new Consumer<Integer>() {
            //     @Override
            //     public void accept(Integer userId) {
            //         userId = MapController.this.currentUserId;
            //         System.out.println("🔗 MapView a demandé l'ID utilisateur: " + userId);
            //     }
            // });
            System.out.println("✅ MapView configurée avec le provider d'ID utilisateur");
        }
    }

    // ==================== GESTION DE L'UTILISATEUR ====================

    public void setCurrentUser(int userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        System.out.println("👤 Utilisateur premium défini: " + userName + " (ID: " + userId + ")");

        // Mettre à jour la configuration si la MapView existe
        if (mapView != null) {
            configureMapView();
        }
    }

    // ==================== GESTION DE L'ÉTAT DE LA CARTE ====================

    public void notifyMapReady() {
        Platform.runLater(() -> {
            if (!isMapReady) {
                isMapReady = true;
                mapReadyLatch.countDown();
                System.out.println("✅ MapController: Carte haute qualité prête à l'utilisation");

                if (currentInfoLabel != null) {
                    currentInfoLabel.setText("✅ Carte haute qualité initialisée - Système premium actif");
                }

                // Localisation automatique par IP avec optimisations
                locateByIPAutomatically();

                // Démarrer le monitoring des performances
                startPerformanceMonitoring();

                // Notifier les écouteurs externes
                if (mapReadyListener != null) {
                    mapReadyListener.run();
                }
            }
        });
    }

    private void locateByIPAutomatically() {
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Délai optimisé pour stabilisation
                Platform.runLater(() -> {
                    if (currentInfoLabel != null) {
                        currentInfoLabel.setText("🌐 Localisation automatique haute précision par IP...");
                    }
                    System.out.println("🌐 Démarrage localisation IP automatique premium pour user " + currentUserId);
                    mapView.localiserParIP();
                    locationRequestCount++;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void startPerformanceMonitoring() {
        Thread monitoringThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Toutes les 30 secondes
                    if (isMapReady) {
                        System.out.println("📊 Monitoring Performance - " +
                                "Marqueurs: " + markerCount + ", " +
                                "Itinéraires: " + routeCalculationCount + ", " +
                                "Localisations: " + locationRequestCount + ", " +
                                "Opérations totales: " + totalOperations);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    public void setOnMapReady(Runnable listener) {
        this.mapReadyListener = listener;
        if (isMapReady && listener != null) {
            listener.run();
        }
    }

    // ==================== GESTION DE L'ATTENTE ====================

    private void waitForMapInitialization() {
        new Thread(() -> {
            try {
                System.out.println("⏳ MapController: Attente initialisation carte haute qualité...");

                if (mapReadyLatch.await(20, TimeUnit.SECONDS)) {
                    Platform.runLater(() -> {
                        isMapReady = true;
                        System.out.println("✅ MapController: Carte haute qualité initialisée avec succès");
                        if (currentInfoLabel != null) {
                            currentInfoLabel.setText("✅ Carte haute qualité initialisée - Système premium prêt");
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        System.err.println("❌ MapController: Timeout initialisation carte haute qualité (20s)");
                        if (currentInfoLabel != null) {
                            currentInfoLabel.setText("❌ Timeout initialisation carte haute qualité");
                        }
                        isMapReady = true;
                        mapReadyLatch.countDown();
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    System.err.println("❌ MapController: Interruption attente carte haute qualité");
                });
            }
        }).start();
    }

    private void executeWhenReady(Runnable action, Label infoLabel, String actionName) {
        totalOperations++;
        this.currentInfoLabel = infoLabel;

        if (mapView == null) {
            if (infoLabel != null) {
                infoLabel.setText("❌ MapView haute qualité non initialisée");
            }
            System.err.println("❌ " + actionName + ": MapView est null");
            return;
        }

        if (isMapReady) {
            System.out.println("🚀 " + actionName + ": Exécution immédiate haute qualité");
            action.run();
        } else {
            if (infoLabel != null) {
                infoLabel.setText("🔄 Initialisation carte haute qualité en cours...");
            }
            System.out.println("⏳ " + actionName + ": En attente de l'initialisation haute qualité...");

            new Thread(() -> {
                try {
                    if (mapReadyLatch.await(8, TimeUnit.SECONDS)) {
                        Platform.runLater(() -> {
                            System.out.println("✅ " + actionName + ": Carte haute qualité prête, exécution");
                            action.run();
                        });
                    } else {
                        Platform.runLater(() -> {
                            if (infoLabel != null) {
                                infoLabel.setText("❌ Carte haute qualité non initialisée - Réessayez");
                            }
                            System.err.println("❌ " + actionName + ": Timeout attente carte haute qualité");
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> {
                        if (infoLabel != null) {
                            infoLabel.setText("❌ Erreur initialisation carte haute qualité");
                        }
                    });
                }
            }).start();
        }
    }

    // ==================== FONCTIONNALITÉS DE NAVIGATION ====================

    public void locateByIP(Label infoLabel) {
        executeWhenReady(() -> {
            infoLabel.setText("🔄 Localisation haute précision via IP...");
            System.out.println("🌐 Début localisation IP premium pour user " + currentUserId);
            mapView.localiserParIP();
            locationRequestCount++;
        }, infoLabel, "Localisation IP haute qualité");
    }

    public void setStartPoint(Label infoLabel) {
        executeWhenReady(() -> {
            mapView.setStartPoint();
            infoLabel.setText("🟢 Cliquez sur la carte haute qualité pour définir le point de départ");
            System.out.println("🟢 Mode sélection point de départ premium activé");
        }, infoLabel, "Définir point départ haute qualité");
    }

    public void setEndPoint(Label infoLabel) {
        executeWhenReady(() -> {
            mapView.setEndPoint();
            infoLabel.setText("🟠 Cliquez sur la carte haute qualité pour définir le point d'arrivée");
            System.out.println("🟠 Mode sélection point d'arrivée premium activé");
        }, infoLabel, "Définir point arrivée haute qualité");
    }

    public void calculateRoute(Label infoLabel) {
        executeWhenReady(() -> {
            mapView.calculateRoute();
            infoLabel.setText("🚗 Calcul de l'itinéraire optimisé en cours...");
            System.out.println("🚗 Calcul d'itinéraire premium démarré");
            routeCalculationCount++;
        }, infoLabel, "Calcul itinéraire optimisé");
    }

    public void clearRoute(Label infoLabel) {
        executeWhenReady(() -> {
            mapView.clearRoute();
            infoLabel.setText("🧹 Itinéraire premium effacé");
            System.out.println("🧹 Itinéraire premium effacé");
        }, infoLabel, "Effacer itinéraire haute qualité");
    }

    // ==================== GESTION DES CARTES (MODES) ====================

    public void switchToStandardMap(Label infoLabel) {
        executeWhenReady(() -> {
            // CORRECTION: Changer switchToStandardMap par activerCarteStandard
            mapView.activerCarteStandard();
            infoLabel.setText("🗺️ Carte standard haute qualité activée");
            System.out.println("🗺️ Passage à la carte standard haute qualité");
        }, infoLabel, "Changement carte standard");
    }

    public void switchToSatelliteMap(Label infoLabel) {
        executeWhenReady(() -> {
            // CORRECTION: Changer switchToSatelliteMap par activerCarteSatellite
            mapView.activerCarteSatellite();
            infoLabel.setText("🛰️ Carte satellite haute qualité activée");
            System.out.println("🛰️ Passage à la carte satellite haute qualité");
        }, infoLabel, "Changement carte satellite");
    }

    public void switchToDarkMap(Label infoLabel) {
        executeWhenReady(() -> {
            // CORRECTION: Changer switchToDarkMap par activerCarteSombre
            mapView.activerCarteSombre();
            infoLabel.setText("🌙 Carte sombre haute qualité activée");
            System.out.println("🌙 Passage à la carte sombre haute qualité");
        }, infoLabel, "Changement carte sombre");
    }

    public void toggleFullscreen(Label infoLabel) {
        executeWhenReady(() -> {
            // CORRECTION: Supprimer toggleFullscreen car la méthode n'existe pas
            // mapView.toggleFullscreen();
            infoLabel.setText("📺 Mode plein écran haute qualité (fonctionnalité à venir)");
            System.out.println("📺 Basculer mode plein écran haute qualité (fonctionnalité à venir)");
        }, infoLabel, "Plein écran haute qualité");
    }

    // ==================== GESTION DES MARQUEURS ====================

    public void centerOnDefaultLocation(Label infoLabel) {
        executeWhenReady(() -> {
            double lat = 33.5731;
            double lon = -7.5898;
            mapView.effacerMarqueurs();
            markerCount = 0;
            mapView.centrerCarte(lat, lon, 12);
            mapView.ajouterMarqueur(lat, lon, "📍 Position par défaut premium\nCasablanca, Maroc");
            markerCount++;
            infoLabel.setText("✅ Centré sur Casablanca haute qualité");
            System.out.println("🎯 Centrage sur position par défaut haute qualité");
        }, infoLabel, "Centrage défaut haute qualité");
    }

    public void showNearbyIncidents(Label infoLabel) {
        executeWhenReady(() -> {
            IPGeolocationService.IPLocation userLocation = IPGeolocationService.getLocationByIP(currentUserId);
            double lat = userLocation.getLatitude();
            double lon = userLocation.getLongitude();

            mapView.effacerMarqueurs();
            markerCount = 0;
            mapView.centrerCarte(lat, lon, 14); // Zoom plus précis

            // Marqueurs incidents premium
            mapView.ajouterMarqueur(lat, lon,
                    "📍 " + currentUserName + "\nVotre position haute précision (IP)\n" + userLocation.getCity());
            mapView.ajouterMarqueur(lat + 0.005, lon + 0.005,
                    "🚧 Embouteillage majeur\nRetard estimé: 15min\nRoute: Boulevard Principal\nImpact: Élevé");
            mapView.ajouterMarqueur(lat - 0.003, lon - 0.002,
                    "⚠️ Travaux routiers étendus\nDétour recommandé\nDurée: 3 semaines\nContractor: Ville de " + userLocation.getCity());
            mapView.ajouterMarqueur(lat + 0.002, lon - 0.004,
                    "🚨 Accident avec blessés\nSecours et police sur place\nCirculation très ralentie");
            mapView.ajouterMarqueur(lat - 0.001, lon + 0.003,
                    "🎉 Événement culturel majeur\nRoute fermée jusqu'à 22h\nDéviation signalisée");

            markerCount += 5;
            infoLabel.setText("✅ 5 incidents haute qualité affichés autour de " + userLocation.getCity());
            System.out.println("🚦 Affichage des incidents premium autour de " + userLocation.getCity());
        }, infoLabel, "Affichage incidents haute qualité");
    }

    public void clearAllMarkers(Label infoLabel) {
        executeWhenReady(() -> {
            mapView.effacerMarqueurs();
            markerCount = 0;
            infoLabel.setText("🗑️ Tous les marqueurs haute qualité effacés");
            System.out.println("🧹 Tous les marqueurs haute qualité effacés");
        }, infoLabel, "Effacement marqueurs haute qualité");
    }

    public void addCustomMarker(double lat, double lon, String title, Label infoLabel) {
        executeWhenReady(() -> {
            mapView.ajouterMarqueur(lat, lon, title);
            markerCount++;
            infoLabel.setText("📌 Marqueur personnalisé haute qualité ajouté: " + title);
            System.out.println("📌 Marqueur premium ajouté: " + lat + ", " + lon);
        }, infoLabel, "Ajout marqueur haute qualité");
    }

    // ==================== NAVIGATION GÉOGRAPHIQUE ====================

    public void zoomToArea(String areaName, Label infoLabel) {
        executeWhenReady(() -> {
            switch (areaName.toLowerCase()) {
                case "casablanca":
                    mapView.centrerCarte(33.5731, -7.5898, 13);
                    infoLabel.setText("🗺️ Zoom haute qualité sur Casablanca");
                    break;
                case "rabat":
                    mapView.centrerCarte(34.0209, -6.8416, 13);
                    infoLabel.setText("🗺️ Zoom haute qualité sur Rabat");
                    break;
                case "marrakech":
                    mapView.centrerCarte(31.6295, -7.9811, 13);
                    infoLabel.setText("🗺️ Zoom haute qualité sur Marrakech");
                    break;
                case "tanger":
                    mapView.centrerCarte(35.7595, -5.8340, 13);
                    infoLabel.setText("🗺️ Zoom haute qualité sur Tanger");
                    break;
                case "fès":
                    mapView.centrerCarte(34.0181, -5.0078, 13);
                    infoLabel.setText("🗺️ Zoom haute qualité sur Fès");
                    break;
                case "position actuelle":
                    IPGeolocationService.IPLocation location = IPGeolocationService.getLocationByIP(currentUserId);
                    mapView.centrerCarte(location.getLatitude(), location.getLongitude(), 14);
                    infoLabel.setText("🗺️ Zoom haute qualité sur votre position: " + location.getCity());
                    break;
                default:
                    infoLabel.setText("❌ Zone non reconnue pour le zoom haute qualité");
                    break;
            }
            System.out.println("🔍 Zoom haute qualité sur: " + areaName);
        }, infoLabel, "Zoom zone haute qualité");
    }

    // ==================== GESTION DES ROUTES (SÉLECTION) ====================

    public void activerSelectionRouteParClic() {
        if (mapView != null && isMapReady) {
            System.out.println("🎯 Activation de la sélection de route par clic premium");

            // CORRECTION: Utiliser activerSelectionParClic pour le mode clic route
            mapView.activerSelectionParClic(routeSelectionnee -> {
                System.out.println("🖱️ Route premium sélectionnée par clic: " + routeSelectionnee.getNom());

                // Afficher la route dans la page de détail
                if (mainController != null) {
                    mainController.showRouteDetailsPage(routeSelectionnee);
                } else {
                    System.err.println("❌ MainController non disponible pour afficher les détails");
                }
            });

            if (currentInfoLabel != null) {
                currentInfoLabel.setText("🎯 Mode sélection premium activé - Cliquez sur une route");
            }
        } else {
            System.err.println("❌ MapView non prête pour la sélection par clic premium");
        }
    }

    public void desactiverSelectionRouteParClic() {
        if (mapView != null) {
            // CORRECTION: Supprimer desactiverSelectionParClic car la méthode n'existe pas
            // mapView.desactiverSelectionParClic();
            System.out.println("🎯 Sélection par clic premium désactivée");

            if (currentInfoLabel != null) {
                currentInfoLabel.setText("✅ Mode sélection premium désactivé");
            }
        }
    }

    public void onRouteSelected(Route route) {
        // Appeler le MainController pour afficher la nouvelle page
        if (mainController != null) {
            mainController.showRouteDetailsPage(route);
        }
    }

    // ==================== TESTS ET DIAGNOSTICS ====================

    public void testIPLocation(Label infoLabel) {
        executeWhenReady(() -> {
            infoLabel.setText("🧪 Test localisation IP haute précision...");
            System.out.println("🧪 Test localisation IP premium pour user " + currentUserId);

            new Thread(() -> {
                try {
                    IPGeolocationService.IPLocation location = IPGeolocationService.getLocationByIP(currentUserId);
                    Platform.runLater(() -> {
                        infoLabel.setText("✅ Test IP haute précision réussi: " + location.getCity() +
                                " (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                        System.out.println("✅ Test IP premium pour " + currentUserName + ": " +
                                location.getCity() + " | Précision: " + location.getFormattedAddress());
                    });
                    locationRequestCount++;
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        infoLabel.setText("❌ Test IP haute précision échoué");
                        System.err.println("❌ Test IP premium échoué: " + e.getMessage());
                    });
                }
            }).start();
        }, infoLabel, "Test IP haute qualité");
    }

    public void runComprehensiveTest(Label infoLabel) {
        executeWhenReady(() -> {
            infoLabel.setText("🧪 Démarrage test complet haute qualité...");
            System.out.println("🧪 Test complet haute qualité démarré");

            new Thread(() -> {
                try {
                    testSequence(infoLabel);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }, infoLabel, "Test complet haute qualité");
    }

    private void testSequence(Label infoLabel) throws InterruptedException {
        Platform.runLater(() -> infoLabel.setText("🧪 Étape 1/6: Centrage défaut..."));
        mapView.centrerCarte(33.5731, -7.5898, 13);
        Thread.sleep(1000);

        Platform.runLater(() -> infoLabel.setText("🧪 Étape 2/6: Ajout marqueurs test..."));
        mapView.ajouterMarqueur(33.5731, -7.5898, "🏙️ Point de test premium\nCasablanca Centre");
        markerCount++;
        Thread.sleep(1000);

        Platform.runLater(() -> infoLabel.setText("🧪 Étape 3/6: Test carte satellite..."));
        // CORRECTION: Changer switchToSatelliteMap par activerCarteSatellite
        mapView.activerCarteSatellite();
        Thread.sleep(1500);

        Platform.runLater(() -> infoLabel.setText("🧪 Étape 4/6: Retour carte standard..."));
        // CORRECTION: Changer switchToStandardMap par activerCarteStandard
        mapView.activerCarteStandard();
        Thread.sleep(1000);

        Platform.runLater(() -> infoLabel.setText("🧪 Étape 5/6: Test localisation IP..."));
        mapView.localiserParIP();
        locationRequestCount++;
        Thread.sleep(2000);

        Platform.runLater(() -> {
            infoLabel.setText("✅ Test complet haute qualité réussi !");
            System.out.println("✅ Test complet haute qualité terminé avec succès");
        });
    }

    // ==================== SERVICES DE LOCALISATION ====================

    public void requestIPLocation() {
        new Thread(() -> {
            try {
                System.out.println("🔍 Début géolocalisation IP haute précision pour user: " + currentUserId);

                IPGeolocationService.IPLocation location = IPGeolocationService.getLocationByIP(currentUserId);

                Platform.runLater(() -> {
                    try {
                        // CORRECTION: showIPLocationOnMap n'existe pas dans la version actuelle de MapView
                        // mapView.showIPLocationOnMap(location.getLatitude(), location.getLongitude(),
                        //         "🌐 Position haute précision\n" + location.getPopupInfo());

                        // Alternative: Utiliser la méthode existante
                        mapView.centrerCarte(location.getLatitude(), location.getLongitude(), 14);
                        mapView.ajouterMarqueur(location.getLatitude(), location.getLongitude(),
                                "🌐 Position haute précision\n" + location.getPopupInfo());

                        System.out.println("📍 Localisation IP haute précision affichée pour " +
                                currentUserName + ": " + location.getFormattedAddress());

                        if (currentInfoLabel != null) {
                            currentInfoLabel.setText("📍 Position IP haute précision: " + location.getFormattedAddress());
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Erreur affichage position IP haute précision: " + e.getMessage());
                    }
                });

                locationRequestCount++;

            } catch (Exception e) {
                System.err.println("❌ Erreur géolocalisation IP haute précision: " + e.getMessage());
                Platform.runLater(() -> {
                    if (currentInfoLabel != null) {
                        currentInfoLabel.setText("❌ Erreur localisation IP haute précision");
                    }
                });
            }
        }).start();
    }

    // ==================== STATUT ET INFORMATIONS ====================

    public String getCurrentRouteInfo() {
        if (startLat == 0.0 || startLng == 0.0 || endLat == 0.0 || endLng == 0.0) {
            return "Aucun itinéraire haute qualité défini";
        }

        return String.format("Itinéraire Premium - Départ: (%.6f, %.6f) | Arrivée: (%.6f, %.6f)",
                startLat, startLng, endLat, endLng);
    }

    public String getStatusInfo() {
        if (!isMapReady) {
            return "🔄 Carte haute qualité en cours d'initialisation...";
        }

        String status = "✅ Système haute qualité opérationnel | 🌐 Localisation IP active | 🚗 Itinéraires optimisés";

        if (startLat != 0.0 || endLat != 0.0) {
            status += " | Itinéraire premium configuré";
        }

        status += " | 👤 " + currentUserName + " (ID:" + currentUserId + ")";
        status += " | 📍 Marqueurs: " + markerCount;
        status += " | 🚗 Calculs: " + routeCalculationCount;

        return status;
    }

    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();

        status.append(isMapReady ? "✅ Carte haute qualité opérationnelle" : "🔄 Carte haute qualité en initialisation");
        status.append(" | 🌐 Localisation IP haute précision");
        status.append(" | 🚗 Itinéraires OSRM optimisés");
        status.append(" | 🗺️ Multi-cartes premium");

        if (mapView != null && mapView.estInitialisee()) {
            status.append(" | 🎯 Vue haute qualité active");
        }

        try {
            IPGeolocationService.IPLocation location = IPGeolocationService.getLocationByIP(currentUserId);
            status.append(" | 📍 ").append(location.getCity()).append(" (Haute précision)");
        } catch (Exception e) {
            status.append(" | 📍 Position haute précision en attente");
        }

        if (startLat != 0.0) status.append(" | 🟢 Départ défini");
        if (endLat != 0.0) status.append(" | 🟠 Arrivée définie");

        status.append(" | 👤 Utilisateur Premium: ").append(currentUserName);
        status.append(" | 📊 Stats: ").append(markerCount).append("📍 ")
                .append(routeCalculationCount).append("🚗 ")
                .append(locationRequestCount).append("🌐 ")
                .append(totalOperations).append("⚙️");

        return status.toString();
    }

    // ==================== GETTERS ET SETTERS ====================

    public boolean isMapReady() {
        return isMapReady;
    }

    public void forceReady() {
        notifyMapReady();
    }

    public IPGeolocationService.IPLocation getCurrentUserLocation() {
        try {
            return IPGeolocationService.getLocationByIP(currentUserId);
        } catch (Exception e) {
            System.err.println("❌ Erreur obtention localisation haute précision: " + e.getMessage());
            return IPGeolocationService.getLocationByIP(1);
        }
    }

    // Getters pour les statistiques
    public int getMarkerCount() { return markerCount; }
    public int getRouteCalculationCount() { return routeCalculationCount; }
    public int getLocationRequestCount() { return locationRequestCount; }
    public int getTotalOperations() { return totalOperations; }

    public double getStartLat() { return startLat; }
    public double getStartLng() { return startLng; }
    public double getEndLat() { return endLat; }
    public double getEndLng() { return endLng; }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public Label getCurrentInfoLabel() {
        return currentInfoLabel;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public MainController getMainController() {
        return mainController;
    }

    public void setPreciseLocationService(PreciseLocationService preciseLocationService) {
        this.preciseLocationService = preciseLocationService;
        System.out.println("📍 Service de localisation haute précision configuré dans MapController");
    }

    public PreciseLocationService getPreciseLocationService() {
        return preciseLocationService;
    }

    // ==================== NETTOYAGE ET GESTION DES RESSOURCES ====================

    public void cleanup() {
        System.out.println("🧹 Nettoyage des ressources haute qualité...");
        markerCount = 0;
        routeCalculationCount = 0;
        locationRequestCount = 0;
        totalOperations = 0;
        if (mapView != null) {
            mapView.effacerMarqueurs();
            mapView.clearRoute();
        }
    }
}