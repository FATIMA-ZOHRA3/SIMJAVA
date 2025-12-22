package com.example.demo.view;

import com.example.demo.controller.MainController;
import com.example.demo.controller.MapController;
import com.example.demo.model.Route;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.text.Font;

public class MainView extends BorderPane {

    // Contrôleurs
    private MainController mainController;
    private MapController mapController;

    // Composants de vue
    private MapView mapView;
    private Label statusLabel;
    private StackPane mapContainer;
    private ScrollPane scrollableControlPanel;

    // Composants de contrôle
    private ComboBox<Route> routesComboBox;
    private Button showRouteButton;
    private Button routeSelectionButton;
    private boolean selectionActive = false;

    // Panels de contrôle
    private VBox controlPanel;
    private HBox topPanel;

    // Panel des options d'itinéraire
    private VBox routeOptionsPanel;
    private CheckBox avoidHighwaysCheck;
    private CheckBox avoidTollsCheck;
    private CheckBox avoidFerriesCheck;
    private ComboBox<String> vehicleTypeCombo;
    private TextField fuelEfficiencyField;
    private TextField fuelPriceField;
    private TextField co2PerKmField;
    private boolean routeOptionsVisible = false;

    // 🎨 OPTIMIZED COLOR SCHEME - MODERN & PROFESSIONAL
    private static final String MAIN_BG = """
        -fx-background-color: linear-gradient(135deg,
            #667eea 0%,
            #764ba2 25%,
            #f093fb 50%,
            #f5576c 75%,
            #4facfe 100%);
        -fx-border-color: linear-gradient(45deg, #667eea, #764ba2);
        -fx-border-width: 2px;
        -fx-border-radius: 15px;
        -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 25, 0.4, 0, 8);
    """;

    private static final String MAP_CONTAINER_STYLE = """
        -fx-background-color: linear-gradient(135deg,
            #0f0c29 0%,
            #302b63 50%,
            #24243e 100%);
        -fx-border-color: linear-gradient(45deg, #ff6b6b, #ffa500, #4ecdc4);
        -fx-border-width: 3px;
        -fx-border-radius: 20px;
        -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.4), 30, 0.5, 0, 10);
    """;

    private static final String READY_STYLE = """
        -fx-background-color: linear-gradient(135deg,
            #667eea 0%,
            #764ba2 25%,
            #f093fb 50%,
            #f5576c 75%,
            #4facfe 100%);
        -fx-border-color: linear-gradient(45deg, #00d2d3, #54a0ff, #5f27cd);
        -fx-border-width: 3px;
        -fx-border-radius: 15px;
        -fx-effect: dropshadow(gaussian, rgba(0, 210, 211, 0.4), 25, 0.4, 0, 8);
    """;

    // 🎨 NEW COLOR CONSTANTS FOR BETTER CONSISTENCY
    private static final String CONTROL_PANEL_BG = """
        -fx-background-color: linear-gradient(to bottom,
            #1e3a8a 0%,
            #1e40af 50%,
            #1e3a8a 100%);
        -fx-background-radius: 15px;
        -fx-border-color: linear-gradient(to bottom, #1e3a8a, #1e40af);
        -fx-border-width: 2px;
        -fx-border-radius: 15px;
        -fx-effect: dropshadow(gaussian, rgba(30, 58, 138, 0.3), 15, 0.3, 0, 5);
    """;

    private static final String STATUS_BAR_READY = """
        -fx-background-color: linear-gradient(135deg,
            rgba(76, 175, 80, 0.95) 0%,
            rgba(139, 195, 74, 0.9) 50%,
            rgba(76, 175, 80, 0.95) 100%);
        -fx-text-fill: white;
        -fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.4), 15, 0.3, 0, 4);
    """;

    private static final String STATUS_BAR_WARNING = """
        -fx-background-color: linear-gradient(135deg,
            rgba(255, 152, 0, 0.95) 0%,
            rgba(245, 124, 0, 0.9) 50%,
            rgba(255, 152, 0, 0.95) 100%);
        -fx-text-fill: white;
        -fx-effect: dropshadow(gaussian, rgba(255, 152, 0, 0.4), 15, 0.3, 0, 4);
    """;

    private static final String STATUS_BAR_ERROR = """
        -fx-background-color: linear-gradient(135deg,
            rgba(244, 67, 54, 0.95) 0%,
            rgba(211, 47, 47, 0.9) 50%,
            rgba(244, 67, 54, 0.95) 100%);
        -fx-text-fill: white;
        -fx-effect: dropshadow(gaussian, rgba(244, 67, 54, 0.4), 15, 0.3, 0, 4);
    """;

    public MainView() {
        super();
        initializeUI();
    }

    // ==================== INITIALISATION DE L'INTERFACE ====================

    private void initializeUI() {
        setPadding(new Insets(15));
        setStyle(MAIN_BG);

        // Création des panneaux
        createTopPanel();
        createControlPanel();
        createMapContainer();
        createRouteOptionsPanel();

        // Configuration initiale
        setTop(topPanel);
        setLeft(scrollableControlPanel);
        setCenter(mapContainer);
        setBottom(createEnhancedStatusLabel());

        // Initialisation différée de MapView
        Platform.runLater(this::initializeMapView);
    }

    // ==================== CRÉATION DES PANNEAUX ====================

    private void createTopPanel() {
        topPanel = new HBox();
        topPanel.setPadding(new Insets(15, 25, 15, 25));
        topPanel.setStyle("""
            -fx-background-color: linear-gradient(135deg,
                rgba(255,255,255,0.95) 0%,
                rgba(248,250,252,0.9) 50%,
                rgba(241,245,249,0.85) 100%);
            -fx-border-color: linear-gradient(45deg, #667eea, #764ba2);
            -fx-border-width: 0 0 3px 0;
            -fx-border-radius: 0 0 15px 15px;
            -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.2), 15, 0.3, 0, 4);
        """);

        Label titleLabel = new Label("🚗 Traffic Monitor Premium - Système Haute Qualité");
        titleLabel.setStyle("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-text-fill: linear-gradient(to right, #667eea, #764ba2);
            -fx-background-color: transparent;
            -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 12, 0.5, 0, 3);
            -fx-padding: 5px 15px;
            -fx-background-radius: 25px;
        """);

        // Add some decorative elements
        Label emojiLabel = new Label("✨");
        emojiLabel.setStyle("""
            -fx-font-size: 24px;
            -fx-text-fill: #f093fb;
            -fx-effect: dropshadow(gaussian, rgba(240, 147, 251, 0.6), 8, 0.4, 0, 2);
        """);

        topPanel.getChildren().addAll(emojiLabel, titleLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
    }

    private void createRouteOptionsPanel() {
        routeOptionsPanel = new VBox(10);
        routeOptionsPanel.setPadding(new Insets(20));
        routeOptionsPanel.setStyle("""
            -fx-background-color: linear-gradient(135deg,
                rgba(255,255,255,0.98) 0%,
                rgba(248,250,252,0.95) 50%,
                rgba(241,245,249,0.92) 100%);
            -fx-background-radius: 20;
            -fx-border-color: linear-gradient(45deg, #667eea, #764ba2, #f093fb);
            -fx-border-width: 3px;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 20, 0.4, 0, 8);
        """);
        routeOptionsPanel.setPrefWidth(300);
        routeOptionsPanel.setMinWidth(300);
        routeOptionsPanel.setMaxWidth(300);
        routeOptionsPanel.setVisible(false);
        routeOptionsPanel.setManaged(false);

        // En-tête
        Label headerLabel = new Label("⚙️ OPTIONS D'ITINÉRAIRE");
        headerLabel.setStyle("""
            -fx-font-weight: bold; 
            -fx-font-size: 16px; 
            -fx-text-fill: #2c3e50;
            -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 5, 0.5, 0, 1);
            -fx-padding: 0 0 10px 0;
        """);

        // Options d'évitement
        Label avoidLabel = new Label("🚧 ÉVITER");
        avoidLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 13px;");

        avoidHighwaysCheck = new CheckBox("Autoroutes");
        avoidTollsCheck = new CheckBox("Péages");
        avoidFerriesCheck = new CheckBox("Ferries");

        // Type de véhicule
        Label vehicleLabel = new Label("🚗 TYPE DE VÉHICULE");
        vehicleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 13px;");

        vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll("Voiture", "Vélo", "Camion", "Moto");
        vehicleTypeCombo.setValue("Voiture");
        vehicleTypeCombo.setPrefWidth(Double.MAX_VALUE);

        // Paramètres écologiques
        Label ecoLabel = new Label("🌱 PARAMÈTRES ÉCOLOGIQUES");
        ecoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 13px;");

        HBox fuelEfficiencyBox = new HBox(10);
        fuelEfficiencyBox.setAlignment(Pos.CENTER_LEFT);
        Label fuelEfficiencyLabel = new Label("⛽ Consommation (L/100km):");
        fuelEfficiencyLabel.setStyle("-fx-font-size: 12px;");
        fuelEfficiencyField = new TextField("7.0");
        fuelEfficiencyField.setPrefWidth(60);
        fuelEfficiencyBox.getChildren().addAll(fuelEfficiencyLabel, fuelEfficiencyField);

        HBox fuelPriceBox = new HBox(10);
        fuelPriceBox.setAlignment(Pos.CENTER_LEFT);
        Label fuelPriceLabel = new Label("💰 Prix carburant (€/L):");
        fuelPriceLabel.setStyle("-fx-font-size: 12px;");
        fuelPriceField = new TextField("1.5");
        fuelPriceField.setPrefWidth(60);
        fuelPriceBox.getChildren().addAll(fuelPriceLabel, fuelPriceField);

        HBox co2Box = new HBox(10);
        co2Box.setAlignment(Pos.CENTER_LEFT);
        Label co2Label = new Label("🌍 CO₂ par km (kg):");
        co2Label.setStyle("-fx-font-size: 12px;");
        co2PerKmField = new TextField("0.12");
        co2PerKmField.setPrefWidth(60);
        co2Box.getChildren().addAll(co2Label, co2PerKmField);

        // Boutons d'action
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button applyButton = new Button("✅ Appliquer");
        styleButton(applyButton, "#27ae60", 12);
        applyButton.setOnAction(e -> applyRouteOptions());

        Button cancelButton = new Button("❌ Annuler");
        styleButton(cancelButton, "#e74c3c", 12);
        cancelButton.setOnAction(e -> toggleRouteOptions());

        buttonBox.getChildren().addAll(applyButton, cancelButton);

        // Ajout des composants
        routeOptionsPanel.getChildren().addAll(
                headerLabel,
                avoidLabel,
                avoidHighwaysCheck,
                avoidTollsCheck,
                avoidFerriesCheck,
                new Separator(),
                vehicleLabel,
                vehicleTypeCombo,
                new Separator(),
                ecoLabel,
                fuelEfficiencyBox,
                fuelPriceBox,
                co2Box,
                new Separator(),
                buttonBox
        );

        // Positionner le panel à droite
        StackPane wrapper = new StackPane(routeOptionsPanel);
        wrapper.setPadding(new Insets(10));
        setRight(wrapper);
    }

    private void createControlPanel() {
        controlPanel = new VBox(12);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setStyle("""
            -fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.97), rgba(245,245,245,0.95));
            -fx-background-radius: 15;
            -fx-border-color: linear-gradient(to bottom, #3498db, #2980b9);
            -fx-border-width: 2px;
            -fx-border-radius: 15;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.3, 0, 5);
        """);
        controlPanel.setPrefWidth(320);
        controlPanel.setMinWidth(320);
        controlPanel.setMaxWidth(320);

        // Créer un ScrollPane pour le VBox
        scrollableControlPanel = new ScrollPane(controlPanel);
        scrollableControlPanel.setFitToWidth(true);
        scrollableControlPanel.setPrefViewportHeight(800);
        scrollableControlPanel.setStyle("""
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-padding: 0;
        """);
        scrollableControlPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollableControlPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label titleLabel = new Label("🚦 CONTRÔLES PRÉMIUM");
        titleLabel.setStyle("""
            -fx-font-weight: bold;
            -fx-font-size: 20px;
            -fx-text-fill: linear-gradient(to right, #667eea, #764ba2);
            -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 10, 0.5, 0, 3);
            -fx-padding: 0 0 15px 0;
            -fx-background-color: linear-gradient(to right, rgba(227, 242, 253, 0.4), rgba(243, 229, 245, 0.4));
            -fx-background-radius: 15px;
            -fx-padding: 10px 20px 15px 20px;
        """);

        // ========== SECTION 1 : GESTION DES ROUTES ==========
        Label routeSectionLabel = createSectionLabel("🛣️ GESTION DES ROUTES");

        routesComboBox = new ComboBox<>();
        routesComboBox.setMaxWidth(Double.MAX_VALUE);
        routesComboBox.setPrefHeight(40);
        routesComboBox.setPromptText("Sélectionnez une route");
        routesComboBox.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #3498db;
            -fx-border-radius: 6px;
            -fx-padding: 8px;
            -fx-font-size: 13px;
        """);

        showRouteButton = new Button("📊 Afficher Détails Route");
        styleButton(showRouteButton, "#2980b9", 14);
        showRouteButton.setPrefHeight(40);
        showRouteButton.setOnAction(e -> {
            if (mainController != null) {
                Route selectedRoute = routesComboBox.getValue();
                if (selectedRoute != null) {
                    mainController.openRouteDetailsWindow(selectedRoute);
                }
            }
        });

        routeSelectionButton = new Button("🎯 Mode Sélection Route");
        styleButton(routeSelectionButton, "#FF9800", 14);
        routeSelectionButton.setPrefHeight(40);
        routeSelectionButton.setOnAction(e -> {
            if (mainController != null && mapController != null) {
                toggleRouteSelection();
            }
        });

        // ========== SECTION 2 : NAVIGATION ITINÉRAIRE ==========
        Label navSectionLabel = createSectionLabel("📍 NAVIGATION ITINÉRAIRE");

        Button ipLocationButton = new Button("🌐 Ma Position IP");
        styleButton(ipLocationButton, "#3498db", 14);
        ipLocationButton.setPrefHeight(40);
        ipLocationButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.localiserParIP();
                updateStatus("🌐 Localisation IP en cours...", "#3498db");
            }
        });

        HBox centerButtonsBox = new HBox(8);
        centerButtonsBox.setMaxWidth(Double.MAX_VALUE);

        Button centerCasablancaButton = new Button("🏙️ Casablanca");
        styleButton(centerCasablancaButton, "#9b59b6", 13);
        centerCasablancaButton.setMinWidth(140);
        centerCasablancaButton.setPrefHeight(35);
        centerCasablancaButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.centrerSurCasablanca();
                updateStatus("📍 Centré sur Casablanca", "#9b59b6");
            }
        });

        Button centerRabatButton = new Button("🏛️ Rabat");
        styleButton(centerRabatButton, "#9b59b6", 13);
        centerRabatButton.setMinWidth(140);
        centerRabatButton.setPrefHeight(35);
        centerRabatButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.centrerSurRabat();
                updateStatus("📍 Centré sur Rabat", "#9b59b6");
            }
        });

        centerButtonsBox.getChildren().addAll(centerCasablancaButton, centerRabatButton);

        // ========== SECTION 3 : GESTION ITINÉRAIRES ==========
        Label itinerarySectionLabel = createSectionLabel("🚗 GESTION D'ITINÉRAIRES");

        HBox itineraryButtonsBox1 = new HBox(8);
        itineraryButtonsBox1.setMaxWidth(Double.MAX_VALUE);

        Button setStartButton = new Button("🟢 Définir Départ");
        styleButton(setStartButton, "#27ae60", 13);
        setStartButton.setMinWidth(140);
        setStartButton.setPrefHeight(35);
        setStartButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.setStartPoint();
                updateStatus("🟢 Cliquez sur la carte pour définir le point de départ", "#27ae60");
            }
        });

        Button setEndButton = new Button("🟠 Définir Arrivée");
        styleButton(setEndButton, "#e67e22", 13);
        setEndButton.setMinWidth(140);
        setEndButton.setPrefHeight(35);
        setEndButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.setEndPoint();
                updateStatus("🟠 Cliquez sur la carte pour définir le point d'arrivée", "#e67e22");
            }
        });

        itineraryButtonsBox1.getChildren().addAll(setStartButton, setEndButton);

        Button calculateRouteButton = new Button("🚗 Calculer Itinéraire");
        styleButton(calculateRouteButton, "#3498db", 14);
        calculateRouteButton.setPrefHeight(40);
        calculateRouteButton.setOnAction(e -> {
            if (mapView != null) {
                // 🔗 APPLIQUER LES OPTIONS D'ITINÉRAIRE ACTUELLES AVANT LE CALCUL
                applyCurrentRouteOptions();
                mapView.calculateRoute();
                updateStatus("🔄 Calcul de l'itinéraire optimisé en cours...", "#3498db");
            }
        });

        Button clearRouteButton = new Button("🧹 Effacer Itinéraire");
        styleButton(clearRouteButton, "#e74c3c", 14);
        clearRouteButton.setPrefHeight(40);
        clearRouteButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.clearRoute();
                updateStatus("🧹 Itinéraire effacé", "#95a5a6");
            }
        });

        // ========== SECTION 4 : ACTIONS ITINÉRAIRES AVANCÉES ==========
        Label advancedRouteSectionLabel = createSectionLabel("⚡ ACTIONS AVANCÉES");

        HBox advancedButtonsBox = new HBox(8);
        advancedButtonsBox.setMaxWidth(Double.MAX_VALUE);

        Button exportRouteButton = new Button("📤 Exporter");
        styleButton(exportRouteButton, "#1abc9c", 13);
        exportRouteButton.setMinWidth(100);
        exportRouteButton.setPrefHeight(35);
        exportRouteButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.exportRoute();
                updateStatus("📤 Export de l'itinéraire en cours...", "#1abc9c");
            }
        });

        Button shareRouteButton = new Button("📤 Partager");
        styleButton(shareRouteButton, "#1abc9c", 13);
        shareRouteButton.setMinWidth(100);
        shareRouteButton.setPrefHeight(35);
        shareRouteButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.shareRoute();
                updateStatus("📤 Partage de l'itinéraire...", "#1abc9c");
            }
        });

        Button routeInfoButton = new Button("📊 Infos");
        styleButton(routeInfoButton, "#1abc9c", 13);
        routeInfoButton.setMinWidth(100);
        routeInfoButton.setPrefHeight(35);
        routeInfoButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.getCurrentRouteInfo();
                updateStatus("📊 Affichage des informations d'itinéraire", "#1abc9c");
            }
        });

        advancedButtonsBox.getChildren().addAll(exportRouteButton, shareRouteButton, routeInfoButton);

        Button routeOptionsButton = new Button("⚙️ Options Itinéraire");
        styleButton(routeOptionsButton, "#FF9800", 14);
        routeOptionsButton.setPrefHeight(40);
        routeOptionsButton.setOnAction(e -> toggleRouteOptions());

        // ========== SECTION 5 : ZOOM ET VUE ==========
        Label zoomSectionLabel = createSectionLabel("🔍 ZOOM ET VUE");

        HBox zoomBox = new HBox(8);
        zoomBox.setMaxWidth(Double.MAX_VALUE);

        Button zoomInButton = new Button("➕ Zoom Avant");
        styleButton(zoomInButton, "#1abc9c", 13);
        zoomInButton.setMinWidth(145);
        zoomInButton.setPrefHeight(35);
        zoomInButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.zoomIn();
                updateStatus("🔍 Zoom avant", "#1abc9c");
            }
        });

        Button zoomOutButton = new Button("➖ Zoom Arrière");
        styleButton(zoomOutButton, "#1abc9c", 13);
        zoomOutButton.setMinWidth(145);
        zoomOutButton.setPrefHeight(35);
        zoomOutButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.zoomOut();
                updateStatus("🔍 Zoom arrière", "#1abc9c");
            }
        });

        zoomBox.getChildren().addAll(zoomInButton, zoomOutButton);

        HBox zoomPresetBox = new HBox(8);
        zoomPresetBox.setMaxWidth(Double.MAX_VALUE);

        Button zoom10Button = new Button("🔍 Zoom 10");
        styleButton(zoom10Button, "#1abc9c", 13);
        zoom10Button.setMinWidth(145);
        zoom10Button.setPrefHeight(35);
        zoom10Button.setOnAction(e -> {
            if (mapView != null) {
                mapView.ajusterEchelle(10);
                updateStatus("🔍 Zoom niveau 10", "#1abc9c");
            }
        });

        Button zoom13Button = new Button("🔍 Zoom 13");
        styleButton(zoom13Button, "#1abc9c", 13);
        zoom13Button.setMinWidth(145);
        zoom13Button.setPrefHeight(35);
        zoom13Button.setOnAction(e -> {
            if (mapView != null) {
                mapView.ajusterEchelle(13);
                updateStatus("🔍 Zoom niveau 13", "#1abc9c");
            }
        });

        zoomPresetBox.getChildren().addAll(zoom10Button, zoom13Button);

        // ========== SECTION 6 : STYLES DE CARTE ==========
        Label mapSectionLabel = createSectionLabel("🗺️ STYLES DE CARTE");

        HBox mapButtonsBox = new HBox(8);
        mapButtonsBox.setMaxWidth(Double.MAX_VALUE);

        Button standardMapButton = new Button("🗺️ Standard");
        styleButton(standardMapButton, "#1abc9c", 13);
        standardMapButton.setMinWidth(95);
        standardMapButton.setPrefHeight(35);
        standardMapButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.switchToStandardMap();
                updateStatus("🗺️ Mode carte standard activé", "#1abc9c");
            }
        });

        Button satelliteMapButton = new Button("🛰️ Satellite");
        styleButton(satelliteMapButton, "#1abc9c", 13);
        satelliteMapButton.setMinWidth(95);
        satelliteMapButton.setPrefHeight(35);
        satelliteMapButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.switchToSatelliteMap();
                updateStatus("🛰️ Mode satellite activé", "#1abc9c");
            }
        });

        Button darkMapButton = new Button("🌙 Sombre");
        styleButton(darkMapButton, "#1abc9c", 13);
        darkMapButton.setMinWidth(95);
        darkMapButton.setPrefHeight(35);
        darkMapButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.switchToDarkMap();
                updateStatus("🌙 Mode sombre activé", "#1abc9c");
            }
        });

        mapButtonsBox.getChildren().addAll(standardMapButton, satelliteMapButton, darkMapButton);

        Button fullscreenButton = new Button("📺 Plein Écran");
        styleButton(fullscreenButton, "#e67e22", 14);
        fullscreenButton.setPrefHeight(40);
        fullscreenButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.toggleFullscreen();
                updateStatus("📺 Mode plein écran activé", "#e67e22");
            }
        });

        // ========== SECTION 7 : MAINTENANCE ==========
        Label cleanupSectionLabel = createSectionLabel("🧹 MAINTENANCE");

        HBox maintenanceButtonsBox1 = new HBox(8);
        maintenanceButtonsBox1.setMaxWidth(Double.MAX_VALUE);

        Button clearMarkersButton = new Button("🗑️ Marqueurs");
        styleButton(clearMarkersButton, "#e74c3c", 13);
        clearMarkersButton.setMinWidth(145);
        clearMarkersButton.setPrefHeight(35);
        clearMarkersButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.effacerMarqueurs();
                updateStatus("🗑️ Marqueurs effacés", "#e74c3c");
            }
        });

        Button resetMapButton = new Button("🔄 Redémarrer");
        styleButton(resetMapButton, "#f39c12", 13);
        resetMapButton.setMinWidth(145);
        resetMapButton.setPrefHeight(35);
        resetMapButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.redemarrerCarte();
                updateStatus("🔄 Carte redémarrée", "#f39c12");
            }
        });

        maintenanceButtonsBox1.getChildren().addAll(clearMarkersButton, resetMapButton);

        Button enableMapClickButton = new Button("🖱️ Mode Clic Carte");
        styleButton(enableMapClickButton, "#9b59b6", 14);
        enableMapClickButton.setPrefHeight(40);
        enableMapClickButton.setOnAction(e -> {
            if (mapView != null) {
                mapView.enableMapClick();
                updateStatus("🖱️ Mode clic activé - Cliquez sur la carte", "#9b59b6");
            }
        });

        // ========== SECTION 8 : TESTS SYSTÈME ==========
        Label testSectionLabel = createSectionLabel("🧪 TESTS SYSTÈME");

        HBox testButtonsBox = new HBox(8);
        testButtonsBox.setMaxWidth(Double.MAX_VALUE);

        Button systemCheckButton = new Button("🔍 Système");
        styleButton(systemCheckButton, "#2c3e50", 13);
        systemCheckButton.setMinWidth(145);
        systemCheckButton.setPrefHeight(35);
        systemCheckButton.setOnAction(e -> {
            if (mainController != null) {
                mainController.verifierEtatSysteme();
            }
        });

        Button networkCheckButton = new Button("🌐 Réseau");
        styleButton(networkCheckButton, "#3498db", 13);
        networkCheckButton.setMinWidth(145);
        networkCheckButton.setPrefHeight(35);
        networkCheckButton.setOnAction(e -> {
            if (mainController != null) {
                mainController.verifierConnexionReseau();
            }
        });

        testButtonsBox.getChildren().addAll(systemCheckButton, networkCheckButton);

        // ========== SECTION 9 : UTILISATEUR ET VOISINS ==========
        Label userSectionLabel = createSectionLabel("👤 UTILISATEUR ET VOISINS");

        Button showNeighborsButton = new Button("🗺️ Afficher Voisins");
        styleButton(showNeighborsButton, "#9b59b6", 14);
        showNeighborsButton.setPrefHeight(40);
        showNeighborsButton.setOnAction(e -> {
            if (mapView != null) {
                double lat = mapView.getStartLat();
                double lng = mapView.getStartLng();
                if (lat != 0.0 && lng != 0.0) {
                    mapView.showNeighbors(lat, lng);
                    updateStatus("🗺️ Affichage des points voisins", "#9b59b6");
                } else {
                    updateStatus("❌ Définissez d'abord un point de départ", "#e74c3c");
                }
            }
        });

        // ========== SECTION 10 : ITINÉRAIRES PROGRAMMATIQUES ==========
        Label programmaticSectionLabel = createSectionLabel("🤖 ITINÉRAIRES PROGRAMMATIQUES");

        HBox programmaticButtonsBox = new HBox(8);
        programmaticButtonsBox.setMaxWidth(Double.MAX_VALUE);

        Button presetRoute1Button = new Button("🏙️ Casablanca → Rabat");
        styleButton(presetRoute1Button, "#8e44ad", 12);
        presetRoute1Button.setMinWidth(145);
        presetRoute1Button.setPrefHeight(35);
        presetRoute1Button.setOnAction(e -> {
            if (mapView != null) {
                mapView.setStartPoint(33.5731, -7.5898); // Casablanca
                mapView.setEndPoint(33.9692, -6.9272);   // Rabat
                // 🔗 APPLIQUER LES OPTIONS D'ITINÉRAIRE ACTUELLES
                applyCurrentRouteOptions();
                
                // Attendre un court instant puis calculer
                new Thread(() -> {
                    try {
                        Thread.sleep(300); // Délai pour laisser le temps aux points d'être définis
                        Platform.runLater(() -> {
                            mapView.calculateRoute();
                            updateStatus("🏙️ Itinéraire Casablanca → Rabat calculé avec options", "#8e44ad");
                        });
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });

        Button presetRoute2Button = new Button("🏜️ Casablanca → Marrakech");
        styleButton(presetRoute2Button, "#8e44ad", 12);
        presetRoute2Button.setMinWidth(145);
        presetRoute2Button.setPrefHeight(35);
        presetRoute2Button.setOnAction(e -> {
            if (mapView != null) {
                mapView.setStartPoint(33.5731, -7.5898); // Casablanca
                mapView.setEndPoint(31.6295, -7.9811);   // Marrakech
                // 🔗 APPLIQUER LES OPTIONS D'ITINÉRAIRE ACTUELLES
                applyCurrentRouteOptions();
                
                // Attendre un court instant puis calculer
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                        Platform.runLater(() -> {
                            mapView.calculateRoute();
                            updateStatus("🏜️ Itinéraire Casablanca → Marrakech calculé avec options", "#8e44ad");
                        });
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });

        programmaticButtonsBox.getChildren().addAll(presetRoute1Button, presetRoute2Button);

        // ========== AJOUT DES COMPOSANTS ==========
        controlPanel.getChildren().addAll(
                titleLabel,

                // Section Routes
                routeSectionLabel, routesComboBox, showRouteButton, routeSelectionButton,

                // Section Navigation
                navSectionLabel, ipLocationButton, centerButtonsBox,

                // Section Itinéraires
                itinerarySectionLabel, itineraryButtonsBox1, calculateRouteButton, clearRouteButton,

                // Section Gestion avancée
                advancedRouteSectionLabel, advancedButtonsBox, routeOptionsButton,

                // Section Zoom
                zoomSectionLabel, zoomBox, zoomPresetBox,

                // Section Styles de carte
                mapSectionLabel, mapButtonsBox, fullscreenButton,

                // Section Maintenance
                cleanupSectionLabel, maintenanceButtonsBox1, enableMapClickButton,

                // Section Tests
                testSectionLabel, testButtonsBox,

                // Section Utilisateur et Voisins
                userSectionLabel, showNeighborsButton,

                // Section Itinéraires programmatiques
                programmaticSectionLabel, programmaticButtonsBox
        );

        // Ajouter un espacement final pour le scrolling
        VBox spacer = new VBox();
        spacer.setMinHeight(20);
        controlPanel.getChildren().add(spacer);
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    private void createMapContainer() {
        mapContainer = new StackPane();
        mapContainer.setStyle(MAP_CONTAINER_STYLE);
        mapContainer.setPrefSize(1200, 800);

        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.rgb(231, 76, 60, 0.3));
        glowEffect.setRadius(25);
        glowEffect.setSpread(0.1);
        mapContainer.setEffect(glowEffect);

        // Label d'attente
        Label waitingLabel = new Label("🔄 Initialisation de la carte haute qualité...");
        waitingLabel.setStyle("""
            -fx-font-size: 16px; 
            -fx-text-fill: #ecf0f1; 
            -fx-font-weight: bold;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.5, 0, 2);
        """);
        mapContainer.getChildren().add(waitingLabel);
    }

    private Label createEnhancedStatusLabel() {
        statusLabel = new Label("Status: Initialisation système haute qualité...");
        statusLabel.setStyle("""
            -fx-font-size: 14px;
            -fx-padding: 15px 25px;
            -fx-font-weight: bold;
            -fx-text-fill: #667eea;
            -fx-background-color: linear-gradient(135deg,
                rgba(255,255,255,0.95) 0%,
                rgba(248,250,252,0.9) 50%,
                rgba(241,245,249,0.85) 100%);
            -fx-background-radius: 30px;
            -fx-border-color: linear-gradient(45deg, #667eea, #764ba2, #f093fb);
            -fx-border-width: 2px;
            -fx-border-radius: 30px;
            -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 18, 0.4, 0, 5);
        """);

        // Enhanced hover effects with dynamic color changes
        statusLabel.setOnMouseEntered(e -> {
            statusLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-padding: 15px 25px;
                -fx-font-weight: bold;
                -fx-text-fill: #764ba2;
                -fx-background-color: linear-gradient(135deg,
                    rgba(255,255,255,0.98) 0%,
                    rgba(227, 242, 253, 0.95) 50%,
                    rgba(243, 229, 245, 0.9) 100%);
                -fx-background-radius: 30px;
                -fx-border-color: linear-gradient(45deg, #764ba2, #f093fb, #f5576c);
                -fx-border-width: 2px;
                -fx-border-radius: 30px;
                -fx-effect: dropshadow(gaussian, rgba(118, 75, 162, 0.4), 20, 0.5, 0, 6);
                -fx-scale-x: 1.01;
                -fx-scale-y: 1.01;
            """);
        });

        statusLabel.setOnMouseExited(e -> {
            statusLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-padding: 15px 25px;
                -fx-font-weight: bold;
                -fx-text-fill: #667eea;
                -fx-background-color: linear-gradient(135deg,
                    rgba(255,255,255,0.95) 0%,
                    rgba(248,250,252,0.9) 50%,
                    rgba(241,245,249,0.85) 100%);
                -fx-background-radius: 30px;
                -fx-border-color: linear-gradient(45deg, #667eea, #764ba2, #f093fb);
                -fx-border-width: 2px;
                -fx-border-radius: 30px;
                -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 18, 0.4, 0, 5);
                -fx-scale-x: 1.0;
                -fx-scale-y: 1.0;
            """);
        });

        return statusLabel;
    }

    // ==================== MÉTHODES D'INITIALISATION ====================

    public void initializeMapView() {
        try {
            System.out.println("🗺️ Initialisation de MapView haute qualité...");

            if (this.mapView == null) {
                this.mapView = new MapView();
            }

            if (mapView != null) {
                mapView.setOnMapReady(() -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ Carte haute qualité prête dans MainView");
                        showReadyState();
                        updateStatus("✅ Carte haute qualité initialisée - Système premium actif", "#2ecc71");

                        // Mettre à jour la combobox des routes
                        if (routesComboBox != null && mapView.getRoutesDisponibles() != null) {
                            routesComboBox.getItems().setAll(mapView.getRoutesDisponibles());
                        }

                        // Configurer les écouteurs d'événements
                        setupMapListeners();
                    });
                });

                setMapView(mapView);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation MapView haute qualité: " + e.getMessage());
            e.printStackTrace();
            updateStatus("❌ Erreur initialisation carte haute qualité", "#e74c3c");
        }
    }

    private void setupMapListeners() {
        // Écouteur pour les données d'itinéraire
        mapView.setOnRouteDataListener(routeData -> {
            Platform.runLater(() -> {
                System.out.println("📊 Données d'itinéraire reçues:");
                System.out.println("   Distance: " + routeData.getFormattedDistance());
                System.out.println("   Temps: " + routeData.getFormattedTime());
                System.out.println("   CO₂: " + routeData.getFormattedCo2());

                // Afficher une notification
                updateStatus("✅ Itinéraire calculé: " + routeData.getFormattedDistance() +
                        " - " + routeData.getFormattedTime(), "#2ecc71");
            });
        });

        // Écouteur pour les clics sur la carte
        mapView.setOnMapClickListener(mapClickData -> {
            Platform.runLater(() -> {
                System.out.println("🖱️ Clic sur la carte:");
                System.out.println("   Lat: " + mapClickData.getLatitude());
                System.out.println("   Lng: " + mapClickData.getLongitude());
                System.out.println("   Adresse: " + mapClickData.getAddress());

                updateStatus("📍 Position: " + mapClickData.getLatitude() + ", " +
                        mapClickData.getLongitude(), "#3498db");
            });
        });

        // 🔹 Configurer l'écouteur pour les clics sur les routes
        mapView.setOnRouteClicked(route -> {
            Platform.runLater(() -> {
                System.out.println("🛣️ Route sélectionnée par clic: " + route.getNom());

                // Mettre à jour la combobox
                routesComboBox.setValue(route);

                // Afficher les détails automatiquement
                if (mainController != null) {
                    mainController.openRouteDetailsWindow(route);
                }

                updateStatus("✅ Route sélectionnée: " + route.getNom(), "#4CAF50");
            });
        });
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
        if (mapView != null && mapView.getMapPanel() != null && mapContainer != null) {
            // Nettoyer avant d'ajouter
            mapContainer.getChildren().clear();
            mapContainer.getChildren().add(mapView.getMapPanel());
            System.out.println("✅ MapView haute qualité attachée à MainView");

            // 🔹 Configurer le listener ici aussi
            if (mapView != null) {
                mapView.setOnRouteClicked(route -> {
                    Platform.runLater(() -> {
                        handleRouteSelected(route);
                    });
                });
            }

            if (mapController != null) {
                mapController.setMapView(mapView);
            }
        }
    }

    // ==================== GESTION DES CONTRÔLEURS ====================

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        System.out.println("✅ MainController défini dans MainView haute qualité");

        // Mettre à jour la combobox si MapView est déjà initialisée
        if (mapView != null && mapView.getRoutesDisponibles() != null) {
            routesComboBox.getItems().setAll(mapView.getRoutesDisponibles());
        }
    }

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
        System.out.println("✅ MapController défini dans MainView haute qualité");

        if (mapView != null && mapController != null) {
            mapController.setMapView(mapView);
        }
    }

    // ==================== GESTION DE L'ÉTAT ====================

    public void updateStatus(String message, String color) {
        if (statusLabel != null) {
            Platform.runLater(() -> {
                statusLabel.setText("Status: " + message);
                String statusStyle = String.format("""
                    -fx-font-size: 13px; -fx-padding: 12px 20px; -fx-font-weight: bold; 
                    -fx-text-fill: %s; 
                    -fx-background-color: linear-gradient(to right, rgba(44, 62, 80, 0.9), rgba(52, 73, 94, 0.9));
                    -fx-background-radius: 25px;
                    -fx-border-color: %s;
                    -fx-border-width: 1.5px;
                    -fx-border-radius: 25px;
                    -fx-effect: dropshadow(gaussian, rgba(%s, 0.4), 15, 0.3, 0, 3);
                """, color, color, color.substring(1));
                statusLabel.setStyle(statusStyle);
            });
        }
    }

    public void setCurrentUser(int userId, String userName) {
        System.out.println("👤 MainView.setCurrentUser haute qualité: " + userName);
        updateStatus("👤 Utilisateur Premium: " + userName + " | 🗺️ Système haute qualité actif", "#2ecc71");

        if (mapView != null) {
            mapView.setUserIdProvider(userIdProvider -> {
                System.out.println("👤 UserIdProvider appelé pour userId: " + userId);
            });
        }
    }

    public void showReadyState() {
        updateStatus("✅ Système haute qualité prêt - Navigation optimisée", "#2ecc71");

        Platform.runLater(() -> {
            setStyle(READY_STYLE);
        });
    }

    // ==================== GESTION DES ROUTES ====================

    private void toggleRouteSelection() {
        if (!selectionActive) {
            if (mapView != null) {
                // 🔹 Utiliser la méthode correcte activerSelectionParClic
                mapView.activerSelectionParClic(route -> {
                    Platform.runLater(() -> {
                        handleRouteSelected(route);
                    });
                });
            }
            routeSelectionButton.setText("✅ Mode Sélection Actif");
            routeSelectionButton.setStyle("""
                -fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049);
                -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px; 
                -fx-font-size: 14px; -fx-background-radius: 8px;
                -fx-border-color: #2e7d32;
                -fx-border-width: 2px;
                -fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.5), 10, 0.3, 0, 3);
            """);
            selectionActive = true;
            updateStatus("🎯 Mode sélection de route activé - Cliquez sur une route", "#FF9800");
        } else {
            // Désactiver la sélection
            routeSelectionButton.setText("🎯 Mode Sélection Route");
            styleButton(routeSelectionButton, "#FF9800", 14);
            selectionActive = false;
            updateStatus("✅ Mode sélection désactivé", "#4CAF50");
        }
    }

    // 🔹 MÉTHODE : Gestion de la sélection de route
    private void handleRouteSelected(Route route) {
        if (route != null) {
            System.out.println("🛣️ Route sélectionnée: " + route.getNom());

            // Mettre à jour la combobox
            routesComboBox.setValue(route);

            // Afficher directement les détails
            if (mainController != null) {
                mainController.openRouteDetailsWindow(route);
            }

            updateStatus("✅ Route sélectionnée: " + route.getNom(), "#4CAF50");

            // Optionnel : Désactiver le mode sélection après sélection
            if (selectionActive) {
                routeSelectionButton.setText("🎯 Mode Sélection Route");
                styleButton(routeSelectionButton, "#FF9800", 14);
                selectionActive = false;
            }
        }
    }

    // ==================== GESTION DES OPTIONS D'ITINÉRAIRE ====================

    private void toggleRouteOptions() {
        routeOptionsVisible = !routeOptionsVisible;
        routeOptionsPanel.setVisible(routeOptionsVisible);
        routeOptionsPanel.setManaged(routeOptionsVisible);

        if (routeOptionsVisible) {
            // Charger les options actuelles
            if (mapView != null) {
                MapView.RouteOptions currentOptions = mapView.getRouteOptions();
                if (currentOptions != null) {
                    avoidHighwaysCheck.setSelected(currentOptions.isAvoidHighways());
                    avoidTollsCheck.setSelected(currentOptions.isAvoidTolls());
                    avoidFerriesCheck.setSelected(currentOptions.isAvoidFerries());
                    vehicleTypeCombo.setValue(currentOptions.getVehicleType());
                    fuelEfficiencyField.setText(String.valueOf(currentOptions.getFuelEfficiency()));
                    fuelPriceField.setText(String.valueOf(currentOptions.getFuelPrice()));
                    co2PerKmField.setText(String.valueOf(currentOptions.getCo2PerKm()));
                }
            }
            updateStatus("⚙️ Options d'itinéraire ouvertes", "#FF9800");
        } else {
            updateStatus("✅ Options d'itinéraire fermées", "#4CAF50");
        }
    }

    // 🔥 CORRECTION CRITIQUE : Méthode pour appliquer les options d'itinéraire
    private void applyRouteOptions() {
        try {
            // Créer les options à partir des contrôles
            MapView.RouteOptions options = new MapView.RouteOptions();
            options.setAvoidHighways(avoidHighwaysCheck.isSelected());
            options.setAvoidTolls(avoidTollsCheck.isSelected());
            options.setAvoidFerries(avoidFerriesCheck.isSelected());
            
            String vehicleType = vehicleTypeCombo.getValue();
            if (vehicleType != null) {
                // Convertir le texte affiché en valeur interne
                switch (vehicleType.toLowerCase()) {
                    case "voiture": options.setVehicleType("voiture"); break;
                    case "vélo": 
                    case "velo": options.setVehicleType("velo"); break;
                    case "camion": options.setVehicleType("camion"); break;
                    case "moto": options.setVehicleType("moto"); break;
                    default: options.setVehicleType("voiture");
                }
            }
            
            options.setFuelEfficiency(Double.parseDouble(fuelEfficiencyField.getText()));
            options.setFuelPrice(Double.parseDouble(fuelPriceField.getText()));
            options.setCo2PerKm(Double.parseDouble(co2PerKmField.getText()));
            
            // 🔥 APPLIQUER DIRECTEMENT LES OPTIONS À MAPVIEW
            if (mapView != null) {
                mapView.updateRouteOptions(options);
                
                // Vérifier si un itinéraire est actif
                boolean hasActiveRoute = checkIfRouteActive();
                
                if (hasActiveRoute) {
                    // Recalculer automatiquement l'itinéraire s'il est actif
                    Platform.runLater(() -> {
                        mapView.calculateRoute();
                        updateStatus("✅ Options appliquées et itinéraire recalculé", "#27ae60");
                    });
                } else {
                    updateStatus("✅ Options appliquées - Définissez un itinéraire pour voir les effets", "#27ae60");
                }
            }
            
            // Fermer le panel après un court délai
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // 1.5 secondes
                    Platform.runLater(this::toggleRouteOptions);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (NumberFormatException e) {
            updateStatus("❌ Erreur: Valeurs numériques invalides", "#e74c3c");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'application des options: " + e.getMessage());
            updateStatus("❌ Erreur lors de l'application des options", "#e74c3c");
        }
    }

    // 🔥 NOUVELLE MÉTHODE : Vérifier si un itinéraire est actif
    private boolean checkIfRouteActive() {
        if (mapView == null) return false;
        try {
            // Tenter d'accéder aux propriétés pour vérifier l'état
            return mapView.hasActiveRoute();
        } catch (Exception e) {
            // Si la méthode n'existe pas, on suppose qu'il n'y a pas d'itinéraire actif
            return false;
        }
    }

    // 🔥 NOUVELLE MÉTHODE : Appliquer automatiquement les options avant tout calcul d'itinéraire
    private void applyCurrentRouteOptions() {
        try {
            if (mapView == null) return;
            
            // Créer les options actuelles
            MapView.RouteOptions currentOptions = new MapView.RouteOptions();
            currentOptions.setAvoidHighways(avoidHighwaysCheck.isSelected());
            currentOptions.setAvoidTolls(avoidTollsCheck.isSelected());
            currentOptions.setAvoidFerries(avoidFerriesCheck.isSelected());
            
            String vehicleType = vehicleTypeCombo.getValue();
            if (vehicleType != null) {
                // Convertir pour correspondance
                switch (vehicleType.toLowerCase()) {
                    case "voiture": currentOptions.setVehicleType("voiture"); break;
                    case "vélo": 
                    case "velo": currentOptions.setVehicleType("velo"); break;
                    case "camion": currentOptions.setVehicleType("camion"); break;
                    case "moto": currentOptions.setVehicleType("moto"); break;
                    default: currentOptions.setVehicleType("voiture");
                }
            }
            
            currentOptions.setFuelEfficiency(Double.parseDouble(fuelEfficiencyField.getText()));
            currentOptions.setFuelPrice(Double.parseDouble(fuelPriceField.getText()));
            currentOptions.setCo2PerKm(Double.parseDouble(co2PerKmField.getText()));
            
            // Appliquer les options
            mapView.updateRouteOptions(currentOptions);
            System.out.println("✅ Options d'itinéraire appliquées automatiquement: " + currentOptions);
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur lors de l'application automatique des options: " + e.getMessage());
            // Utiliser les valeurs par défaut
            MapView.RouteOptions defaultOptions = new MapView.RouteOptions();
            if (mapView != null) {
                mapView.updateRouteOptions(defaultOptions);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue lors de l'application des options: " + e.getMessage());
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void styleButton(Button button, String color, int fontSize) {
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, %s, darken(%s, 20%%));
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px; 
            -fx-font-size: %dpx; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 30%%);
            -fx-border-width: 1px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.3, 0, 2);
        """, color, color, fontSize, color));

        // Effet hover
        button.setOnMouseEntered(e -> button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, lighten(%s, 10%%), %s);
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px; 
            -fx-font-size: %dpx; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 20%%);
            -fx-border-width: 1px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0.4, 0, 3);
        """, color, color, fontSize, color)));

        button.setOnMouseExited(e -> button.setStyle(String.format("""
            -fx-background-color: linear-gradient(to bottom, %s, darken(%s, 20%%));
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-padding: 10px; 
            -fx-font-size: %dpx; 
            -fx-background-radius: 8px;
            -fx-border-color: darken(%s, 30%%);
            -fx-border-width: 1px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.3, 0, 2);
        """, color, color, fontSize, color)));
    }

    public void attendreInitialisationCarte() {
        if (mapView != null) {
            mapView.waitForInitialization();
        }
    }

    public void resetMap() {
        if (mapView != null) {
            mapView.redemarrerCarte();
        }
    }

    public void resetView() {
        if (mapContainer != null) {
            mapContainer.getChildren().clear();
        }
        if (mapView != null) {
            mapView.effacerMarqueurs();
            mapView.clearRoute();
        }
        updateStatus("Système haute qualité réinitialisé", "#f39c12");
    }

    public void cleanup() {
        System.out.println("🧹 Nettoyage des ressources haute qualité...");
        if (mapView != null) {
            mapView.effacerMarqueurs();
            mapView.clearRoute();
        }
        updateStatus("🧹 Ressources haute qualité nettoyées", "#bdc3c7");
    }

    // ==================== GETTERS ====================

    public Label getInfoLabel() { return statusLabel; }
    public MapView getMapView() { return mapView; }
    public StackPane getMapContainer() { return mapContainer; }
    public ScrollPane getScrollableControlPanel() { return scrollableControlPanel; }
    public String getCurrentStatus() {
        return statusLabel != null ? statusLabel.getText().replace("Status: ", "") : "";
    }

    public ComboBox<Route> getRoutesComboBox() { return routesComboBox; }
    public Button getShowRouteButton() { return showRouteButton; }
    public boolean isSelectionActive() { return selectionActive; }
    public boolean isRouteOptionsVisible() { return routeOptionsVisible; }

    // ==================== GESTION DU LAYOUT ====================

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (mapView != null && mapContainer != null) {
            mapView.setPrefSize(mapContainer.getWidth(), mapContainer.getHeight());
        }
        // Ajuster la hauteur du ScrollPane
        if (scrollableControlPanel != null) {
            scrollableControlPanel.setPrefViewportHeight(getHeight() - topPanel.getHeight() - 100);
        }
    }
}