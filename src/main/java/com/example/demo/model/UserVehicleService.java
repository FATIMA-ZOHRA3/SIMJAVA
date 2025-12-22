package com.example.demo.model;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UserVehicleService {

    private Connection connection;
    private int currentUserId;
    private int currentVehicleId;

    public UserVehicleService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Associer un véhicule à l'utilisateur connecté
     */
    public boolean assignVehicleToUser(int userId, String userEmail) {
        try {
            this.currentUserId = userId;

            // Vérifier si l'utilisateur a déjà un véhicule
            String checkSql = "SELECT id FROM vehicule WHERE user_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    currentVehicleId = rs.getInt("id");
                    System.out.println("✅ Véhicule existant trouvé pour l'utilisateur: " + currentVehicleId);
                    return true;
                }
            }

            // Créer un nouveau véhicule pour l'utilisateur
            String insertSql = "INSERT INTO vehicule (route_id, position_index, vitesse, etat, user_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                Random random = new Random();

                // Route aléatoire au Maroc
                int randomRoute = random.nextInt(10) + 1;
                int randomPosition = random.nextInt(5) + 1;
                int randomSpeed = 60 + random.nextInt(40); // 60-100 km/h

                insertStmt.setInt(1, randomRoute);
                insertStmt.setInt(2, randomPosition);
                insertStmt.setInt(3, randomSpeed);
                insertStmt.setString(4, "EN_MOUVEMENT");
                insertStmt.setInt(5, userId);

                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            currentVehicleId = generatedKeys.getInt(1);
                            System.out.println("✅ Nouveau véhicule créé: " + currentVehicleId + " pour utilisateur: " + userEmail);
                            return true;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur assignation véhicule: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtenir la position actuelle du véhicule utilisateur
     */
    public Map<String, Double> getCurrentVehiclePosition() {
        Map<String, Double> position = new HashMap<>();

        try {
            String sql = """
                SELECT rp.lat, rp.lon 
                FROM vehicule v 
                JOIN route_points rp ON v.route_id = rp.route_id AND v.position_index = rp.point_index 
                WHERE v.id = ?
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, currentVehicleId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    position.put("lat", rs.getDouble("lat"));
                    position.put("lon", rs.getDouble("lon"));
                    return position;
                }
            }

            // Fallback: utiliser une ville marocaine aléatoire
            return getRandomMoroccoPosition();

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération position véhicule: " + e.getMessage());
            return getRandomMoroccoPosition();
        }
    }

    /**
     * Mettre à jour la position du véhicule
     */
    public void updateVehiclePosition(int newRouteId, int newPositionIndex) {
        try {
            String sql = "UPDATE vehicule SET route_id = ?, position_index = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, newRouteId);
                stmt.setInt(2, newPositionIndex);
                stmt.setInt(3, currentVehicleId);
                stmt.executeUpdate();

                System.out.printf("✅ Position véhicule %d mise à jour: route=%d, position=%d%n",
                        currentVehicleId, newRouteId, newPositionIndex);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour position véhicule: " + e.getMessage());
        }
    }

    /**
     * Obtenir les événements de trafic proches
     */
    public void getNearbyTrafficEvents(double lat, double lng, double radiusKm, Label infoLabel) {
        new Thread(() -> {
            try {
                // Événements de trafic dans la zone
                String trafficSql = """
                    SELECT type, lat, lon, description, severity,
                           (6371 * acos(cos(radians(?)) * cos(radians(lat)) * 
                           cos(radians(lon) - radians(?)) + sin(radians(?)) * sin(radians(lat)))) AS distance
                    FROM traffic_event 
                    HAVING distance < ?
                    ORDER BY distance, severity DESC
                    LIMIT 10
                """;

                StringBuilder eventsInfo = new StringBuilder();
                eventsInfo.append("🚦 Événements à proximité:\\n");

                try (PreparedStatement stmt = connection.prepareStatement(trafficSql)) {
                    stmt.setDouble(1, lat);
                    stmt.setDouble(2, lng);
                    stmt.setDouble(3, lat);
                    stmt.setDouble(4, radiusKm);

                    ResultSet rs = stmt.executeQuery();

                    int eventCount = 0;
                    while (rs.next()) {
                        String type = rs.getString("type");
                        @SuppressWarnings("unused")
                        double eventLat = rs.getDouble("lat"); // 🔹 Variable conservée mais non utilisée
                        @SuppressWarnings("unused")
                        double eventLon = rs.getDouble("lon"); // 🔹 Variable conservée mais non utilisée
                        String description = rs.getString("description");
                        @SuppressWarnings("unused")
                        int severity = rs.getInt("severity"); // 🔹 Variable conservée mais non utilisée
                        double distance = rs.getDouble("distance");

                        eventsInfo.append(String.format(
                                "• %s (%.1f km): %s%n",
                                getEventEmoji(type), distance, description
                        ));
                        eventCount++;
                    }

                    if (eventCount == 0) {
                        eventsInfo.append("✅ Aucun événement détecté - Circulation fluide");
                    }

                    final String finalInfo = eventsInfo.toString();
                    Platform.runLater(() -> {
                        if (infoLabel != null) {
                            infoLabel.setText(finalInfo);
                        }
                        System.out.println("🔍 " + finalInfo.replace("\n", " "));
                    });
                }

            } catch (SQLException e) {
                System.err.println("❌ Erreur recherche événements: " + e.getMessage());
                Platform.runLater(() -> {
                    if (infoLabel != null) {
                        infoLabel.setText("❌ Erreur chargement événements");
                    }
                });
            }
        }).start();
    }

    /**
     * Obtenir l'état des routes proches
     */
    public void getNearbyRoutesStatus(double lat, double lng, double radiusKm) {
        new Thread(() -> {
            try {
                String routesSql = """
                    SELECT nom, type_route, vitesse_max,
                           (6371 * acos(cos(radians(?)) * cos(radians(lat)) * 
                           cos(radians(lon) - radians(?)) + sin(radians(?)) * sin(radians(lat)))) AS distance
                    FROM route r
                    JOIN route_points rp ON r.id = rp.route_id
                    GROUP BY r.id
                    HAVING MIN(distance) < ?
                    ORDER BY distance
                    LIMIT 5
                """;

                try (PreparedStatement stmt = connection.prepareStatement(routesSql)) {
                    stmt.setDouble(1, lat);
                    stmt.setDouble(2, lng);
                    stmt.setDouble(3, lat);
                    stmt.setDouble(4, radiusKm);

                    ResultSet rs = stmt.executeQuery();

                    System.out.println("🛣️ Routes à proximité:");
                    while (rs.next()) {
                        String routeName = rs.getString("nom");
                        String routeType = rs.getString("type_route");
                        int maxSpeed = rs.getInt("vitesse_max");
                        double distance = rs.getDouble("distance");

                        System.out.printf("   • %s (%s) - %.1f km - %d km/h%n",
                                routeName, routeType, distance, maxSpeed);
                    }
                }

            } catch (SQLException e) {
                System.err.println("❌ Erreur recherche routes: " + e.getMessage());
            }
        }).start();
    }

    // Méthodes utilitaires
    private Map<String, Double> getRandomMoroccoPosition() {
        Map<String, Double> position = new HashMap<>();
        Random random = new Random();

        double[][] moroccoCities = {
                {33.5731, -7.5898}, // Casablanca
                {34.0209, -6.8416}, // Rabat
                {31.6295, -7.9811}, // Marrakech
                {34.0181, -5.0078}, // Fès
                {35.7595, -5.8340}, // Tanger
                {30.4278, -9.5981}  // Agadir
        };

        int index = random.nextInt(moroccoCities.length);
        position.put("lat", moroccoCities[index][0]);
        position.put("lon", moroccoCities[index][1]);

        return position;
    }

    private String getEventEmoji(String type) {
        return switch (type.toLowerCase()) {
            case "accident" -> "💥 Accident";
            case "embouteillage" -> "🚗 Embouteillage";
            case "travaux" -> "🚧 Travaux";
            case "police" -> "👮 Police";
            default -> "⚠️ " + type;
        };
    }

    // Getters
    public int getCurrentVehicleId() { return currentVehicleId; }
    public int getCurrentUserId() { return currentUserId; }
}