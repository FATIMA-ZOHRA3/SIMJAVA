package com.example.demo.model;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PreciseLocationService {
    private final Connection connection;
    private final Map<String, double[]> roadCache;

    // Bounding box du Maroc
    private static final double MOROCCO_MIN_LAT = 27.5;
    private static final double MOROCCO_MAX_LAT = 36.0;
    private static final double MOROCCO_MIN_LON = -13.5;
    private static final double MOROCCO_MAX_LON = -1.0;

    // Routes principales du Maroc (coordonnées approximatives)
    private static final Map<String, double[]> MOROCCAN_ROADS = new HashMap<>();

    static {
        MOROCCAN_ROADS.put("A1_CASABLANCA_RABAT", new double[]{33.5731, -7.5898});
        MOROCCAN_ROADS.put("A2_RABAT_FES", new double[]{34.0209, -6.8416});
        MOROCCAN_ROADS.put("A3_FES_OUJDA", new double[]{33.8869, -5.5470});
        MOROCCAN_ROADS.put("A4_MEKNES_TANGER", new double[]{34.2610, -6.5802});
        MOROCCAN_ROADS.put("RN1_CASABLANCA_MARRAKECH", new double[]{31.6295, -8.0089});
        MOROCCAN_ROADS.put("RN2_RABAT_FES", new double[]{33.9716, -6.8498});
        MOROCCAN_ROADS.put("RN6_AGADIR_ESSAOUIRA", new double[]{30.4278, -9.5981});
        MOROCCAN_ROADS.put("RN8_OUARZAZATE_AGADIR", new double[]{30.9335, -6.9370});
        MOROCCAN_ROADS.put("RN9_MARRAKECH_OUARZAZATE", new double[]{31.5085, -9.7595});
        MOROCCAN_ROADS.put("RN10_AGADIR_TIZNIT", new double[]{29.6969, -9.7316});
        MOROCCAN_ROADS.put("RN12_SAFI_EL_JADIDA", new double[]{32.2994, -9.2372});
        MOROCCAN_ROADS.put("RN13_BENI_MELLAL_MARRAKECH", new double[]{32.3373, -6.3498});
    }

    public PreciseLocationService(Connection connection) {
        this.connection = connection;
        this.roadCache = new ConcurrentHashMap<>();
        initializeTable();
    }

    private void initializeTable() {
        try (Statement stmt = connection.createStatement()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS user_precise_locations (
                    user_id INT PRIMARY KEY,
                    lat DOUBLE NOT NULL,
                    lon DOUBLE NOT NULL,
                    precision_m INT DEFAULT 50,
                    source VARCHAR(50) DEFAULT 'GPS',
                    snapped_to_road BOOLEAN DEFAULT FALSE,
                    road_name VARCHAR(255),
                    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """;
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur création table positions: " + e.getMessage());
        }
    }

    public boolean isInMorocco(double lat, double lon) {
        return lat >= MOROCCO_MIN_LAT && lat <= MOROCCO_MAX_LAT &&
                lon >= MOROCCO_MIN_LON && lon <= MOROCCO_MAX_LON;
    }

    public Map<String, Object> snapToNearestRoad(double lat, double lon) {
        String cacheKey = String.format("%.4f,%.4f", lat, lon);
        if (roadCache.containsKey(cacheKey)) {
            double[] cached = roadCache.get(cacheKey);
            Map<String, Object> result = new HashMap<>();
            result.put("lat", cached[0]);
            result.put("lon", cached[1]);
            result.put("snapped", true);
            result.put("road_name", "Route Principale (Cached)");
            return result;
        }

        double nearestLat = lat;
        double nearestLon = lon;
        String nearestRoadName = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> road : MOROCCAN_ROADS.entrySet()) {
            String roadKey = road.getKey();
            double[] roadCoords = road.getValue();
            double distance = calculateDistance(lat, lon, roadCoords[0], roadCoords[1]);

            if (distance < minDistance && distance < 10000) {
                minDistance = distance;
                nearestLat = roadCoords[0];
                nearestLon = roadCoords[1];
                nearestRoadName = extractRoadName(roadKey);
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (nearestRoadName != null && minDistance < 10000) {
            double[] adjusted = adjustToRoad(lat, lon, nearestLat, nearestLon, minDistance);
            result.put("lat", adjusted[0]);
            result.put("lon", adjusted[1]);
            result.put("snapped", true);
            result.put("road_name", nearestRoadName);
            roadCache.put(cacheKey, new double[]{adjusted[0], adjusted[1]});
        } else {
            result.put("lat", lat);
            result.put("lon", lon);
            result.put("snapped", false);
            result.put("road_name", "Hors Route");
        }
        return result;
    }

    private double[] adjustToRoad(double originalLat, double originalLon, double roadLat, double roadLon, double distance) {
        if (distance < 100) return new double[]{roadLat, roadLon};
        double ratio = 50.0 / distance;
        double adjustedLat = originalLat + (roadLat - originalLat) * ratio;
        double adjustedLon = originalLon + (roadLon - originalLon) * ratio;
        return new double[]{adjustedLat, adjustedLon};
    }

    private String extractRoadName(String roadKey) {
        if (roadKey.startsWith("A")) {
            return "Autoroute " + roadKey.split("_")[0] + " " +
                    roadKey.substring(roadKey.indexOf('_') + 1).replace("_", "-");
        } else if (roadKey.startsWith("RN")) {
            return "Route Nationale " + roadKey.split("_")[0].substring(2) + " " +
                    roadKey.substring(roadKey.indexOf('_') + 1).replace("_", "-");
        }
        return roadKey.replace("_", " ");
    }

    // Méthode principale avec 5 paramètres
    public void saveUserLocation(int userId, double lat, double lon, int precision, String source) {
        if (!isInMorocco(lat, lon)) {
            System.err.println("Localisation hors du Maroc: " + lat + ", " + lon);
            return;
        }
        Map<String, Object> snappedLocation = snapToNearestRoad(lat, lon);

        double finalLat = (Double) snappedLocation.get("lat");
        double finalLon = (Double) snappedLocation.get("lon");
        boolean isSnapped = (Boolean) snappedLocation.get("snapped");
        String roadName = (String) snappedLocation.get("road_name");

        String sql = """
            INSERT INTO user_precise_locations (user_id, lat, lon, precision_m, source, snapped_to_road, road_name)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                lat=VALUES(lat), lon=VALUES(lon), precision_m=VALUES(precision_m),
                source=VALUES(source), snapped_to_road=VALUES(snapped_to_road),
                road_name=VALUES(road_name), last_updated=NOW()
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDouble(2, finalLat);
            ps.setDouble(3, finalLon);
            ps.setInt(4, precision);
            ps.setString(5, source);
            ps.setBoolean(6, isSnapped);
            ps.setString(7, roadName);
            ps.executeUpdate();

            System.out.println("Position sauvegardée pour user " + userId +
                    " sur " + roadName + " (" + finalLat + ", " + finalLon + ")");
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde position: " + e.getMessage());
        }
    }

    // Surcharge pour compatibilité avec 4 paramètres (par défaut source = "GPS")
    public void saveUserLocation(int userId, double lat, double lon, int precision) {
        saveUserLocation(userId, lat, lon, precision, "GPS");
    }

    public Map<String, Object> getUserLocation(int userId) {
        String sql = "SELECT * FROM user_precise_locations WHERE user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> loc = new HashMap<>();
                loc.put("lat", rs.getDouble("lat"));
                loc.put("lon", rs.getDouble("lon"));
                loc.put("precision", rs.getInt("precision_m"));
                loc.put("source", rs.getString("source"));
                loc.put("snapped_to_road", rs.getBoolean("snapped_to_road"));
                loc.put("road_name", rs.getString("road_name"));
                return loc;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture position: " + e.getMessage());
        }
        return null;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }

    public void clearCache() {
        roadCache.clear();
    }

    public int getCacheSize() {
        return roadCache.size();
    }
}
