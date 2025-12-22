package com.example.demo.view;

import com.example.demo.model.Route;
import com.example.demo.model.Vehicle;
import com.example.demo.model.FeuCirculation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RouteCanvas extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private Route routeActuelle;
    private boolean simulationEnCours = false;
    private final Random random = new Random();
    private AnimationTimer animationTimer;
    private long lastUpdateTime = 0;
    private double timeScale = 1.0;
    private boolean showRealTimeInfo = true;

    // 🔥 GESTION MULTI-ROUTES
    private Map<String, RouteState> etatsRoutes = new HashMap<>();
    private RouteState etatCourant;
    private AtomicInteger tempsGlobal = new AtomicInteger(0);

    // 🔥 CLASSE POUR L'ÉTAT D'UNE ROUTE
    private class RouteState {
        String routeId;
        Route route;
        Map<Vehicle, VehicleBehavior> vehicules = new HashMap<>();
        List<FeuCirculation> feux = new ArrayList<>();
        TrafficStatistics stats;
        int tempsRoute = 0;

        RouteState(String routeId, Route route) {
            this.routeId = routeId;
            this.route = route;
            this.stats = new TrafficStatistics();
            initialiserRoute();
        }

        void initialiserRoute() {
            // Initialiser les feux
            if (route.getFeuxCirculation().isEmpty()) {
                creerFeuxAdaptes();
            } else {
                feux = new ArrayList<>(route.getFeuxCirculation());
            }

            // Initialiser les véhicules
            for (int i = 0; i < route.getVehicules().size(); i++) {
                Vehicle v = route.getVehicules().get(i);
                vehicules.put(v, new VehicleBehavior(v, routeId, i, route.getVehicules().size()));
            }

            System.out.println("✅ Route " + routeId + " initialisée: " +
                    vehicules.size() + " véhicules, " + feux.size() + " feux");
        }

        void creerFeuxAdaptes() {
            feux.clear();
            int nombreFeux;

            // Adapter le nombre de feux au type de route
            String nom = route.getNom().toLowerCase();
            if (nom.contains("autoroute") || nom.contains("rapide")) {
                nombreFeux = 2;
            } else if (nom.contains("urbain") || nom.contains("ville")) {
                nombreFeux = 4;
            } else {
                nombreFeux = 3;
            }

            for (int i = 0; i < nombreFeux; i++) {
                FeuCirculation feu = new FeuCirculation(
                        "feu_" + routeId + "_" + (i + 1),
                        0, 0, // Coordonnées mises à jour dans le dessin
                        "feu_tricolore",
                        routeId,
                        "Feu " + (i + 1)
                );

                // Timing réaliste
                feu.setTempsRouge(30 + random.nextInt(15));
                feu.setTempsVert(25 + random.nextInt(15));
                feu.setTempsOrange(4);

                // Synchronisation initiale
                int delai = i * 8;
                int cycle = feu.getTempsRouge() + feu.getTempsVert() + 4;
                int position = delai % cycle;

                if (position < feu.getTempsRouge()) {
                    feu.setEtat("rouge");
                    feu.setCompteurTemps(feu.getTempsRouge() - position);
                } else if (position < feu.getTempsRouge() + 4) {
                    feu.setEtat("orange");
                    feu.setCompteurTemps(feu.getTempsRouge() + 4 - position);
                } else {
                    feu.setEtat("vert");
                    feu.setCompteurTemps(cycle - position);
                }

                feux.add(feu);
            }

            route.getFeuxCirculation().clear();
            route.getFeuxCirculation().addAll(feux);
        }

        void animerFeux() {
            for (FeuCirculation feu : feux) {
                if (feu.getCompteurTemps() > 0) {
                    feu.decrementerCompteur();
                    continue;
                }

                switch (feu.getEtat().toLowerCase()) {
                    case "rouge":
                        feu.setEtat("vert");
                        feu.setCompteurTemps(feu.getTempsVert());
                        break;
                    case "vert":
                        feu.setEtat("orange");
                        feu.setCompteurTemps(feu.getTempsOrange());
                        break;
                    case "orange":
                        feu.setEtat("rouge");
                        feu.setCompteurTemps(feu.getTempsRouge());
                        break;
                }
            }
        }

        void mettreAJourVehicules(double deltaTime) {
            // Mettre à jour chaque véhicule
            for (Map.Entry<Vehicle, VehicleBehavior> entry : vehicules.entrySet()) {
                VehicleBehavior behavior = entry.getValue();
                behavior.mettreAJour(deltaTime, feux, tempsRoute);
            }

            // Mettre à jour les statistiques
            stats.mettreAJour(vehicules.values(), deltaTime);
            tempsRoute++;
        }
    }

    // 🔥 COMPORTEMENT DES VÉHICULES
    private class VehicleBehavior {
        Vehicle vehicle;
        String routeId;
        int index;
        double currentSpeed;
        double targetSpeed;
        double positionRoute;
        double laneOffset;
        FeuCirculation feuProche = null;
        double distanceFeu = Double.MAX_VALUE;
        boolean isBraking = false;
        int tempsArret = 0;
        double acceleration = 0;

        VehicleBehavior(Vehicle vehicle, String routeId, int index, int totalVehicules) {
            this.vehicle = vehicle;
            this.routeId = routeId;
            this.index = index;
            this.currentSpeed = vehicle.getVitesse();
            this.positionRoute = (index * 0.9) / Math.max(1, totalVehicules);

            // Vitesse cible selon le type de véhicule
            switch (vehicle.getType().toLowerCase()) {
                case "camion":
                    this.targetSpeed = 80;
                    this.laneOffset = -0.8;
                    break;
                case "bus":
                    this.targetSpeed = 70;
                    this.laneOffset = -0.4;
                    break;
                case "voiture_sport":
                    this.targetSpeed = 120;
                    this.laneOffset = 0.8;
                    break;
                case "moto":
                    this.targetSpeed = 100;
                    this.laneOffset = random.nextDouble() * 1.6 - 0.8;
                    break;
                default:
                    this.targetSpeed = 90;
                    this.laneOffset = random.nextDouble() * 1.0 - 0.5;
            }

            // Ajuster selon le type de route
            if (routeId.contains("urbain")) this.targetSpeed *= 0.7;
            if (routeId.contains("autoroute")) this.targetSpeed *= 1.3;
        }

        void mettreAJour(double deltaTime, List<FeuCirculation> feux, int tempsRoute) {
            // Trouver le feu le plus proche
            trouverFeuProche(feux);

            // Appliquer le comportement
            if (feuProche != null) {
                gererComportementFeu(deltaTime);
            } else {
                gererConduiteNormale(deltaTime);
            }

            // Mettre à jour la position
            positionRoute += (currentSpeed / 100.0) * deltaTime * timeScale;
            if (positionRoute > 1.0) {
                positionRoute = 0;
            }

            // Changement de voie occasionnel
            if (currentSpeed > 30 && random.nextDouble() < 0.001) {
                laneOffset += (random.nextDouble() - 0.5) * 0.3;
                laneOffset = Math.max(-1.0, Math.min(1.0, laneOffset));
            }
        }

        void trouverFeuProche(List<FeuCirculation> feux) {
            feuProche = null;
            distanceFeu = Double.MAX_VALUE;

            for (FeuCirculation feu : feux) {
                int indexFeu = feux.indexOf(feu);
                double positionFeu = (indexFeu + 1.0) / (feux.size() + 1);
                double distance = Math.abs(positionRoute - positionFeu);

                if (distance < 0.2 && distance < distanceFeu) {
                    distanceFeu = distance;
                    feuProche = feu;
                }
            }
        }

        void gererComportementFeu(double deltaTime) {
            if (feuProche == null) return;

            String etat = feuProche.getEtat().toLowerCase();
            int tempsRestant = feuProche.getCompteurTemps();

            switch (etat) {
                case "rouge":
                    gererFeuRouge(deltaTime, tempsRestant);
                    break;
                case "orange":
                    gererFeuOrange(deltaTime, tempsRestant);
                    break;
                case "vert":
                    gererFeuVert(deltaTime);
                    break;
            }
        }

        void gererFeuRouge(double deltaTime, int tempsRestant) {
            if (distanceFeu < 0.05) {
                isBraking = true;

                if (distanceFeu < 0.02) {
                    // Arrêt complet
                    currentSpeed = 0;
                    tempsArret = tempsRestant;
                } else {
                    // Freinage progressif
                    double forceFreinage = 5.0 * (0.05 - distanceFeu) / 0.05;
                    acceleration = -forceFreinage;
                    currentSpeed += acceleration * deltaTime * 3.6;
                    currentSpeed = Math.max(0, currentSpeed);
                }
            } else {
                gererConduiteNormale(deltaTime);
            }
        }

        void gererFeuOrange(double deltaTime, int tempsRestant) {
            if (distanceFeu < 0.04) {
                if (distanceFeu < 0.015 || currentSpeed > 60 || tempsRestant < 2) {
                    // Passer à l'orange
                    acceleration = 0;
                } else {
                    // S'arrêter
                    acceleration = -3.0;
                    currentSpeed += acceleration * deltaTime * 3.6;
                    currentSpeed = Math.max(0, currentSpeed);
                }
            } else {
                gererConduiteNormale(deltaTime);
            }
        }

        void gererFeuVert(double deltaTime) {
            isBraking = false;
            tempsArret = 0;

            if (distanceFeu < 0.03) {
                // Accélérer pour traverser
                acceleration = Math.min(2.0, acceleration + 0.5);
            }

            gererConduiteNormale(deltaTime);
        }

        void gererConduiteNormale(double deltaTime) {
            double diffVitesse = targetSpeed - currentSpeed;
            acceleration = diffVitesse * 0.1;
            acceleration = Math.max(-6.0, Math.min(3.0, acceleration));

            currentSpeed += acceleration * deltaTime * 3.6;
            currentSpeed = Math.max(0, Math.min(currentSpeed, 180));
        }
    }

    // 🔥 STATISTIQUES
    private class TrafficStatistics {
        double vitesseMoyenne = 0;
        int vehiculesArretes = 0;
        int vehiculesTotal = 0;
        int passagesFeu = 0;

        void mettreAJour(Iterable<VehicleBehavior> vehicules, double deltaTime) {
            double totalVitesse = 0;
            int count = 0;
            int arretes = 0;

            for (VehicleBehavior vb : vehicules) {
                totalVitesse += vb.currentSpeed;
                count++;
                if (vb.currentSpeed < 5) arretes++;
                if (vb.tempsArret > 0) passagesFeu++;
            }

            vitesseMoyenne = count > 0 ? totalVitesse / count : 0;
            vehiculesArretes = arretes;
            vehiculesTotal = count;
        }
    }

    // 🔥 CONSTRUCTEUR
    public RouteCanvas(double width, double height) {
        System.out.println("🛣️ RouteCanvas initialisé: " + width + "x" + height);

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        canvas.setWidth(width);
        canvas.setHeight(height);
        setPrefSize(width, height);

        setStyle("-fx-border-color: #2c3e50; -fx-border-width: 2px; -fx-background-color: #f8f9fa;");

        dessinerMessageAttente();
    }

    // 🔥 MÉTHODE PRINCIPALE : CHANGER DE ROUTE
    public void setRoute(Route nouvelleRoute) {
        if (nouvelleRoute == null) {
            dessinerMessageAttente();
            return;
        }

        String routeId = nouvelleRoute.getId();
        System.out.println("🔄 Chargement route: " + nouvelleRoute.getNom() + " (ID: " + routeId + ")");

        // Arrêter l'animation actuelle
        if (animationTimer != null) {
            animationTimer.stop();
        }

        // Sauvegarder l'état courant si nécessaire
        if (etatCourant != null) {
            etatsRoutes.put(etatCourant.routeId, etatCourant);
        }

        // Charger ou créer l'état de la nouvelle route
        if (etatsRoutes.containsKey(routeId)) {
            etatCourant = etatsRoutes.get(routeId);
            routeActuelle = etatCourant.route;
        } else {
            etatCourant = new RouteState(routeId, nouvelleRoute);
            etatsRoutes.put(routeId, etatCourant);
            routeActuelle = nouvelleRoute;
        }

        // Redémarrer l'animation si la simulation était en cours
        if (simulationEnCours) {
            demarrerAnimationRealiste();
        }

        redessinerRoute();
    }

    // 🔥 DÉMARRER LA SIMULATION
    public void demarrerSimulation() {
        if (etatCourant == null) {
            System.out.println("⚠️ Aucune route sélectionnée");
            return;
        }

        System.out.println("▶ Simulation démarrée pour route: " + etatCourant.routeId);
        simulationEnCours = true;

        if (animationTimer == null) {
            demarrerAnimationRealiste();
        } else {
            animationTimer.start();
        }

        redessinerRoute();
    }

    // 🔥 ARRÊTER LA SIMULATION
    public void arreterSimulation() {
        System.out.println("⏸ Simulation arrêtée");
        simulationEnCours = false;

        if (animationTimer != null) {
            animationTimer.stop();
        }

        redessinerRoute();
    }

    // 🔥 ANIMATION
    private void demarrerAnimationRealiste() {
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

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                if (simulationEnCours && etatCourant != null) {
                    // Mettre à jour les feux chaque seconde
                    if (now - lastUpdateTime > 1_000_000_000) {
                        etatCourant.animerFeux();
                        lastUpdateTime = now;
                        tempsGlobal.incrementAndGet();
                    }

                    // Mettre à jour les véhicules
                    etatCourant.mettreAJourVehicules(deltaTime);

                    // Redessiner
                    if (showRealTimeInfo) {
                        redessinerRoute();
                    }
                }
            }
        };

        animationTimer.start();
    }

    // 🎨 DESSIN DE LA ROUTE
    private void redessinerRoute() {
        if (etatCourant == null) {
            dessinerMessageAttente();
            return;
        }

        try {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // Fond
            gc.setFill(Color.rgb(245, 245, 245));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // En-tête avec infos route
            dessinerEntete();

            // Route principale
            double routeY = canvas.getHeight() / 2;
            double routeHeight = 120;
            dessinerRoute(routeY, routeHeight);

            // Feux de circulation
            dessinerFeux(routeY, routeHeight);

            // Véhicules
            if (simulationEnCours) {
                dessinerVehicules(routeY, routeHeight);
            }

            // Informations
            if (showRealTimeInfo) {
                dessinerInformations();
            }

            // Départ et arrivée
            dessinerDepartArrivee(routeY, routeHeight);

        } catch (Exception e) {
            System.err.println("❌ Erreur dessin: " + e.getMessage());
            dessinerMessageErreur("Erreur: " + e.getMessage());
        }
    }

    private void dessinerEntete() {
        // Bandeau supérieur
        gc.setFill(Color.rgb(50, 50, 150));
        gc.fillRect(0, 0, canvas.getWidth(), 50);

        // Titre route
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        String titre = "🛣️ " + routeActuelle.getNom();
        if (etatCourant != null) {
            titre += " (ID: " + etatCourant.routeId + ")";
        }
        gc.fillText(titre, 20, 30);

        // État simulation
        gc.setFont(Font.font("Arial", 12));
        String etat = simulationEnCours ? "▶ EN COURS" : "⏸ PAUSÉE";
        Color couleurEtat = simulationEnCours ? Color.GREEN : Color.ORANGE;
        gc.setFill(couleurEtat);
        gc.fillText(etat, canvas.getWidth() - 100, 30);
    }

    private void dessinerRoute(double routeY, double routeHeight) {
        double routeWidth = canvas.getWidth() - 100;
        double routeX = 50;

        // Chaussée principale
        gc.setFill(Color.rgb(100, 100, 100));
        gc.fillRoundRect(routeX, routeY - routeHeight/2, routeWidth, routeHeight, 10, 10);

        // Accotements
        gc.setFill(Color.rgb(80, 80, 80));
        gc.fillRect(routeX, routeY - routeHeight/2, routeWidth, 5);
        gc.fillRect(routeX, routeY + routeHeight/2 - 5, routeWidth, 5);

        // Ligne médiane discontinue
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(3);
        gc.setLineDashes(20, 15);
        gc.strokeLine(routeX, routeY, routeX + routeWidth, routeY);
        gc.setLineDashes(null);

        // Lignes de voies
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double voieGaucheY = routeY - routeHeight/4;
        double voieDroiteY = routeY + routeHeight/4;
        gc.strokeLine(routeX, voieGaucheY, routeX + routeWidth, voieGaucheY);
        gc.strokeLine(routeX, voieDroiteY, routeX + routeWidth, voieDroiteY);

        // Flèches directionnelles
        for (int i = 0; i < 5; i++) {
            double x = routeX + 50 + i * 150;
            if (x < routeX + routeWidth - 50) {
                dessinerFleche(x, routeY, 30, true);
                dessinerFleche(x, voieGaucheY, 20, true);
                dessinerFleche(x, voieDroiteY, 20, true);
            }
        }
    }

    private void dessinerFeux(double routeY, double routeHeight) {
        if (etatCourant == null || etatCourant.feux.isEmpty()) return;

        double routeWidth = canvas.getWidth() - 100;
        double routeX = 50;

        for (int i = 0; i < etatCourant.feux.size(); i++) {
            FeuCirculation feu = etatCourant.feux.get(i);
            double position = (i + 1.0) / (etatCourant.feux.size() + 1);
            double x = routeX + position * routeWidth;
            double y = routeY - routeHeight/2 - 40;

            dessinerFeuTricolore(x, y, feu, i);
        }
    }

    private void dessinerFeuTricolore(double x, double y, FeuCirculation feu, int index) {
        String etat = feu.getEtat().toLowerCase();

        // Support
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(x - 2, y, 4, 40);

        // Boîtier
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(x - 20, y - 60, 40, 60, 5, 5);

        // Lumière ROUGE
        gc.setFill(etat.equals("rouge") ? Color.RED : Color.rgb(60, 0, 0));
        gc.fillOval(x - 10, y - 55, 20, 16);

        // Lumière ORANGE
        gc.setFill(etat.equals("orange") ? Color.ORANGE : Color.rgb(60, 35, 0));
        gc.fillOval(x - 10, y - 37, 20, 16);

        // Lumière VERTE
        gc.setFill(etat.equals("vert") ? Color.GREEN : Color.rgb(0, 50, 0));
        gc.fillOval(x - 10, y - 19, 20, 16);

        // Effet lumineux pour l'état actif
        if (etat.equals("rouge") || etat.equals("orange") || etat.equals("vert")) {
            Color effet = etat.equals("rouge") ? Color.rgb(255, 100, 100, 0.3) :
                    etat.equals("orange") ? Color.rgb(255, 200, 100, 0.3) :
                            Color.rgb(100, 255, 100, 0.3);
            gc.setFill(effet);
            gc.fillOval(x - 15, y - 60, 30, 30);
        }

        // Numéro et timer
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.fillText((index + 1) + ":" + feu.getCompteurTemps() + "s", x - 15, y - 65);
    }

    private void dessinerVehicules(double routeY, double routeHeight) {
        if (etatCourant == null) return;

        double routeWidth = canvas.getWidth() - 100;
        double routeX = 50;

        int index = 0;
        for (Map.Entry<Vehicle, VehicleBehavior> entry : etatCourant.vehicules.entrySet()) {
            VehicleBehavior behavior = entry.getValue();
            Vehicle vehicle = entry.getKey();

            double x = routeX + behavior.positionRoute * routeWidth;
            double y = routeY + behavior.laneOffset * (routeHeight / 3);
            y = Math.max(routeY - routeHeight/2 + 20, Math.min(routeY + routeHeight/2 - 20, y));

            dessinerVehicule(x, y, vehicle.getType(), index, behavior);
            index++;
        }
    }

    private void dessinerVehicule(double x, double y, String type, int index, VehicleBehavior behavior) {
        // Couleur selon type
        Color couleur;
        switch (type.toLowerCase()) {
            case "camion": couleur = Color.rgb(120, 120, 120); break;
            case "bus": couleur = Color.rgb(220, 180, 0); break;
            case "taxi": couleur = Color.rgb(240, 240, 0); break;
            case "voiture_sport": couleur = Color.rgb(200, 0, 0); break;
            case "moto": couleur = Color.rgb(0, 100, 200); break;
            case "velo": couleur = Color.rgb(0, 150, 0); break;
            default: couleur = Color.rgb(100, 150, 200);
        }

        // Taille selon type
        int width, height;
        switch (type.toLowerCase()) {
            case "camion": width = 45; height = 20; break;
            case "bus": width = 42; height = 18; break;
            case "voiture_sport": width = 30; height = 14; break;
            case "moto": width = 20; height = 10; break;
            case "velo": width = 15; height = 8; break;
            default: width = 35; height = 16;
        }

        // Corps du véhicule
        gc.setFill(couleur);
        gc.fillRoundRect(x - width/2, y - height/2, width, height, 5, 5);

        // Vitres
        gc.setFill(Color.rgb(180, 220, 255, 0.7));
        gc.fillRect(x - width/2 + 3, y - height/2 + 3, width - 6, height/3);

        // Roues
        gc.setFill(Color.BLACK);
        int wheelSize = Math.min(5, height/2);
        gc.fillOval(x - width/2 + 3, y + height/2 - wheelSize, wheelSize, wheelSize);
        gc.fillOval(x + width/2 - wheelSize - 3, y + height/2 - wheelSize, wheelSize, wheelSize);

        // Feux stop si freinage
        if (behavior.isBraking) {
            gc.setFill(Color.RED);
            gc.fillOval(x + width/2 - 4, y - 3, 4, 4);
            gc.fillOval(x + width/2 - 4, y + 3, 4, 4);
        }

        // Vitesse
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 9));
        String speedText = String.format("%.0f", behavior.currentSpeed) + "km/h";
        gc.fillText(speedText, x - width/2, y - height/2 - 5);

        // ID
        gc.setFill(Color.DARKBLUE);
        gc.fillText("#" + (index + 1), x - width/2, y + height/2 + 12);

        // Indicateur d'arrêt au feu
        if (behavior.tempsArret > 0) {
            gc.setFill(Color.RED);
            gc.fillText("🛑" + behavior.tempsArret + "s", x - 10, y - height/2 - 20);
        }
    }

    private void dessinerInformations() {
        if (etatCourant == null) return;

        // Panneau d'informations
        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        gc.fillRoundRect(20, 60, 280, 160, 10, 10);
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1);
        gc.strokeRoundRect(20, 60, 280, 160, 10, 10);

        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("📊 STATISTIQUES", 30, 80);

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 12));

        int y = 100;
        gc.fillText("Route: " + routeActuelle.getNom(), 30, y);
        y += 20;
        gc.fillText("Véhicules: " + etatCourant.vehicules.size(), 30, y);
        y += 20;
        gc.fillText("Feux: " + etatCourant.feux.size(), 30, y);
        y += 20;

        if (etatCourant.stats != null) {
            gc.fillText("Vitesse moy: " + String.format("%.1f", etatCourant.stats.vitesseMoyenne) + " km/h", 30, y);
            y += 20;
            gc.fillText("Arrêtés: " + etatCourant.stats.vehiculesArretes, 30, y);
            y += 20;
            gc.fillText("Temps route: " + etatCourant.tempsRoute + "s", 30, y);
        }

        // État simulation
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setFill(simulationEnCours ? Color.GREEN : Color.RED);
        gc.fillText(simulationEnCours ? "▶ SIMULATION EN COURS" : "⏸ SIMULATION PAUSÉE", 30, y + 30);

        // Vitesse simulation
        gc.setFill(Color.GRAY);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("Vitesse: x" + String.format("%.1f", timeScale), 30, y + 50);
    }

    private void dessinerDepartArrivee(double routeY, double routeHeight) {
        double routeWidth = canvas.getWidth() - 100;
        double routeX = 50;

        // Départ
        gc.setFill(Color.GREEN);
        gc.fillRoundRect(routeX - 25, routeY - 20, 50, 40, 8, 8);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText("DÉPART", routeX - 20, routeY + 5);

        // Arrivée
        gc.setFill(Color.BLUE);
        gc.fillRoundRect(routeX + routeWidth - 25, routeY - 20, 50, 40, 8, 8);
        gc.setFill(Color.WHITE);
        gc.fillText("ARRIVÉE", routeX + routeWidth - 22, routeY + 5);
    }

    private void dessinerFleche(double x, double y, double size, boolean forward) {
        gc.setFill(Color.WHITE);

        double[] xPoints, yPoints;
        if (forward) {
            xPoints = new double[]{x - size/2, x + size/2, x};
            yPoints = new double[]{y, y, y - size/2};
        } else {
            xPoints = new double[]{x - size/2, x + size/2, x};
            yPoints = new double[]{y, y, y + size/2};
        }

        gc.fillPolygon(xPoints, yPoints, 3);
    }

    private void dessinerMessageAttente() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText("🛣️ SIMULATEUR DE TRAFIC", canvas.getWidth()/2 - 120, canvas.getHeight()/2 - 20);

        gc.setFont(Font.font("Arial", 14));
        gc.setFill(Color.GRAY);
        gc.fillText("Sélectionnez une route pour commencer", canvas.getWidth()/2 - 130, canvas.getHeight()/2 + 20);
    }

    private void dessinerMessageErreur(String message) {
        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("❌ " + message, 20, 30);
    }

    // 🎮 MÉTHODES DE CONTRÔLE
    public void ajusterVitesseSimulation(double facteur) {
        timeScale = Math.max(0.1, Math.min(5.0, facteur));
        if (etatCourant != null) {
            System.out.println("⏩ Route " + etatCourant.routeId + " - Vitesse: x" + timeScale);
        }
    }

    public void activerInfosTempsReel(boolean activer) {
        showRealTimeInfo = activer;
        redessinerRoute();
    }

    public void resetSimulation() {
        if (etatCourant != null) {
            System.out.println("🔄 Réinitialisation route: " + etatCourant.routeId);
            etatCourant.initialiserRoute();
            redessinerRoute();
        }
    }

    public void zoomIn() {
        canvas.setScaleX(canvas.getScaleX() * 1.2);
        canvas.setScaleY(canvas.getScaleY() * 1.2);
        redessinerRoute();
    }

    public void zoomOut() {
        canvas.setScaleX(canvas.getScaleX() / 1.2);
        canvas.setScaleY(canvas.getScaleY() / 1.2);
        redessinerRoute();
    }

    public void resetView() {
        canvas.setScaleX(1.0);
        canvas.setScaleY(1.0);
        redessinerRoute();
    }

    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();

        if (etatCourant != null && etatCourant.stats != null) {
            stats.put("routeId", etatCourant.routeId);
            stats.put("routeNom", routeActuelle.getNom());
            stats.put("vehiculesTotal", etatCourant.stats.vehiculesTotal);
            stats.put("vehiculesArretes", etatCourant.stats.vehiculesArretes);
            stats.put("vitesseMoyenne", etatCourant.stats.vitesseMoyenne);
            stats.put("feuxActifs", etatCourant.feux.size());
            stats.put("tempsSimulation", etatCourant.tempsRoute);
            stats.put("passagesFeu", etatCourant.stats.passagesFeu);
        }

        return stats;
    }

    public void cleanup() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }

        simulationEnCours = false;
        etatCourant = null;
        routeActuelle = null;

        System.out.println("🧹 RouteCanvas nettoyé");
    }

    // GETTERS
    public String getRouteIdActuelle() {
        return etatCourant != null ? etatCourant.routeId : null;
    }

    public boolean isSimulationEnCours() {
        return simulationEnCours;
    }

    public Route getRouteActuelle() {
        return routeActuelle;
    }
}