package com.example.demo.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class IPGeolocationService {
    private static final String[] API_ENDPOINTS = {
            "http://ip-api.com/json/",
            "https://ipapi.co/json/"
    };

    // 🔹 SIMULATION DE POSITIONS POUR DIFFÉRENTS UTILISATEURS
    private static final Map<Integer, IPLocation> userSimulatedLocations = new HashMap<>();
    private static final Random random = new Random();

    static {
        // Positions simulées pour différents utilisateurs
        userSimulatedLocations.put(1, new IPLocation(33.5731, -7.5898, "Casablanca", "Morocco", "MA",
                "Casablanca-Settat", "Maroc Telecom", "20000", "simulation-1"));
        userSimulatedLocations.put(2, new IPLocation(34.0209, -6.8416, "Rabat", "Morocco", "MA",
                "Rabat-Salé-Kénitra", "Maroc Telecom", "10000", "simulation-2"));
        userSimulatedLocations.put(3, new IPLocation(31.6295, -7.9811, "Marrakech", "Morocco", "MA",
                "Marrakech-Safi", "Maroc Telecom", "40000", "simulation-3"));
        userSimulatedLocations.put(4, new IPLocation(33.8869, -7.6200, "Témara", "Morocco", "MA",
                "Rabat-Salé-Kénitra", "INWI", "12100", "simulation-4"));
        userSimulatedLocations.put(5, new IPLocation(33.2353, -8.5000, "El Jadida", "Morocco", "MA",
                "Casablanca-Settat", "Orange Maroc", "24000", "simulation-5"));

        // 🔹 AJOUT DE PLUS D'UTILISATEURS POUR ÉVITER LES DOUBLONS
        userSimulatedLocations.put(6, new IPLocation(34.2610, -6.5802, "Kénitra", "Morocco", "MA",
                "Rabat-Salé-Kénitra", "INWI", "14000", "simulation-6"));
        userSimulatedLocations.put(7, new IPLocation(32.2990, -9.2372, "Safi", "Morocco", "MA",
                "Marrakech-Safi", "Orange Maroc", "46000", "simulation-7"));
        userSimulatedLocations.put(8, new IPLocation(35.7595, -5.8340, "Tanger", "Morocco", "MA",
                "Tanger-Tétouan-Al Hoceïma", "Maroc Telecom", "90000", "simulation-8"));
        userSimulatedLocations.put(9, new IPLocation(34.9250, -2.4340, "Oujda", "Morocco", "MA",
                "Oriental", "INWI", "60000", "simulation-9"));
        userSimulatedLocations.put(10, new IPLocation(30.4278, -9.5981, "Agadir", "Morocco", "MA",
                "Souss-Massa", "Orange Maroc", "80000", "simulation-10"));
    }

    public static IPLocation getLocationByIP() {
        return getLocationByIP(0); // Par défaut, utilisateur 0 (position réelle)
    }

    public static IPLocation getLocationByIP(int userId) {
        // 🔹 SI MODE SIMULATION ACTIVÉ, RETOURNER UNE POSITION SIMULÉE
        if (isSimulationMode() && userId > 0) {
            IPLocation simulatedLocation = getSimulatedLocation(userId);
            if (simulatedLocation != null) {
                System.out.println("🎭 Mode simulation - Utilisateur " + userId + " à " + simulatedLocation.getCity());
                return simulatedLocation;
            }
        }

        // 🔹 SINON, UTILISER LA GÉOLOCALISATION IP RÉELLE
        for (String apiUrl : API_ENDPOINTS) {
            try {
                IPLocation location = fetchLocationFromAPI(apiUrl);
                if (location != null) {
                    System.out.println("📍 Localisation IP réelle: " + location.getCity());
                    return location;
                }
            } catch (Exception e) {
                System.err.println("❌ Échec avec " + apiUrl + ": " + e.getMessage());
            }
        }

        // 🔹 FALLBACK VERS POSITION PAR DÉFAUT
        return getDefaultLocation();
    }

    private static IPLocation getSimulatedLocation(int userId) {
        // 🔹 CORRECTION : Utiliser modulo pour éviter les IDs hors limites
        int safeUserId = userId;
        if (userId > 10 || userId < 1) {
            safeUserId = (userId % 10) + 1; // Recycler entre 1 et 10
            System.out.println("🔄 Recyclage ID utilisateur " + userId + " -> " + safeUserId);
        }

        // Retourner une position simulée pour l'utilisateur
        IPLocation baseLocation = userSimulatedLocations.get(safeUserId);

        if (baseLocation == null) {
            // Fallback sécurisé
            baseLocation = userSimulatedLocations.get(1);
            System.out.println("⚠️  ID " + safeUserId + " non trouvé, utilisation Casablanca");
        }

        // 🔹 AJOUTER UN PEU D'ALÉATOIRE POUR RENDRE PLUS RÉALISTE
        double latVariation = (random.nextDouble() - 0.5) * 0.01; // ±0.005 degrés
        double lonVariation = (random.nextDouble() - 0.5) * 0.01; // ±0.005 degrés

        return new IPLocation(
                baseLocation.getLatitude() + latVariation,
                baseLocation.getLongitude() + lonVariation,
                baseLocation.getCity(),
                baseLocation.getCountry(),
                baseLocation.getCountryCode(),
                baseLocation.getRegion(),
                baseLocation.getIsp(),
                baseLocation.getZipCode(),
                "simulation-" + userId
        );
    }

    private static boolean isSimulationMode() {
        // 🔹 ACTIVER LA SIMULATION POUR LA DÉMONSTRATION
        return true;
    }

    // 🔹 NOUVELLE MÉTHODE POUR OBTENIR UNE VILLE ALÉATOIRE
    public static IPLocation getRandomLocation(int userId) {
        String[] cities = {
                "Casablanca", "Rabat", "Marrakech", "Témara", "El Jadida",
                "Kénitra", "Safi", "Tanger", "Oujda", "Agadir",
                "Fès", "Meknès", "Tétouan", "Nador", "Mohammedia"
        };

        double[][] coordinates = {
                {33.5731, -7.5898}, // Casablanca
                {34.0209, -6.8416}, // Rabat
                {31.6295, -7.9811}, // Marrakech
                {33.8869, -7.6200}, // Témara
                {33.2353, -8.5000}, // El Jadida
                {34.2610, -6.5802}, // Kénitra
                {32.2990, -9.2372}, // Safi
                {35.7595, -5.8340}, // Tanger
                {34.9250, -2.4340}, // Oujda
                {30.4278, -9.5981}, // Agadir
                {34.0181, -5.0078}, // Fès
                {33.8920, -5.5540}, // Meknès
                {35.5710, -5.3720}, // Tétouan
                {35.1740, -2.9286}, // Nador
                {33.6833, -7.3833}  // Mohammedia
        };

        String[] regions = {
                "Casablanca-Settat", "Rabat-Salé-Kénitra", "Marrakech-Safi",
                "Rabat-Salé-Kénitra", "Casablanca-Settat", "Rabat-Salé-Kénitra",
                "Marrakech-Safi", "Tanger-Tétouan-Al Hoceïma", "Oriental",
                "Souss-Massa", "Fès-Meknès", "Fès-Meknès", "Tanger-Tétouan-Al Hoceïma",
                "Oriental", "Casablanca-Settat"
        };

        String[] isps = {"Maroc Telecom", "INWI", "Orange Maroc"};

        int index = (userId - 1) % cities.length;

        double lat = coordinates[index][0] + (random.nextDouble() - 0.5) * 0.01;
        double lon = coordinates[index][1] + (random.nextDouble() - 0.5) * 0.01;
        String city = cities[index];
        String region = regions[index];
        String isp = isps[random.nextInt(isps.length)];

        return new IPLocation(lat, lon, city, "Morocco", "MA", region, isp, "00000", "random-" + userId);
    }

    private static IPLocation fetchLocationFromAPI(String apiUrl) throws Exception {
        // 🔹 CORRECTION: Utiliser URI au lieu de URL directement
        URL url = new URI(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Java IP Geolocation)");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        String json = response.toString();

        if (apiUrl.contains("ip-api.com")) {
            if (json.contains("\"status\":\"success\"")) {
                double lat = extractDouble(json, "\"lat\":");
                double lon = extractDouble(json, "\"lon\":");
                String city = extractString(json, "\"city\":");
                String country = extractString(json, "\"country\":");
                String countryCode = extractString(json, "\"countryCode\":");
                String region = extractString(json, "\"regionName\":");
                String isp = extractString(json, "\"isp\":");
                String zip = extractString(json, "\"zip\":");

                System.out.println("📍 IP-API: " + city + ", " + country + " (" + lat + ", " + lon + ")");

                return new IPLocation(
                        lat, lon, city, country, countryCode,
                        region, isp, zip, "ip-api.com"
                );
            }
        } else if (apiUrl.contains("ipapi.co")) {
            if (!json.contains("\"error\"")) {
                double lat = extractDouble(json, "\"latitude\":");
                double lon = extractDouble(json, "\"longitude\":");
                String city = extractString(json, "\"city\":");
                String country = extractString(json, "\"country_name\":");
                String countryCode = extractString(json, "\"country_code\":");
                String region = extractString(json, "\"region\":");
                String isp = extractString(json, "\"org\":");
                String zip = extractString(json, "\"postal\":");

                System.out.println("📍 IPAPI: " + city + ", " + country + " (" + lat + ", " + lon + ")");

                return new IPLocation(
                        lat, lon, city, country, countryCode,
                        region, isp, zip, "ipapi.co"
                );
            }
        }

        return null;
    }

    private static double extractDouble(String json, String key) {
        try {
            int start = json.indexOf(key) + key.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String value = json.substring(start, end).trim();
            return Double.parseDouble(value);
        } catch (Exception e) {
            System.err.println("❌ Erreur extraction double pour: " + key);
            return 0.0;
        }
    }

    private static String extractString(String json, String key) {
        try {
            int start = json.indexOf(key) + key.length();
            start = json.indexOf("\"", start) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            System.err.println("❌ Erreur extraction string pour: " + key);
            return "Inconnu";
        }
    }

    private static IPLocation getDefaultLocation() {
        System.out.println("📍 Utilisation position par défaut: Casablanca");
        return new IPLocation(33.5731, -7.5898, "Casablanca", "Morocco", "MA",
                "Casablanca-Settat", "Maroc Telecom", "20000", "default");
    }

    // 🔹 MÉTHODE POUR CHANGER LE MODE SIMULATION
    public static void setSimulationMode(boolean enabled) {
        System.out.println("🎭 Mode simulation: " + (enabled ? "ACTIVÉ" : "DÉSACTIVÉ"));
    }

    // 🔹 MÉTHODE POUR TESTER LA SIMULATION
    public static void testSimulation() {
        System.out.println("🧪 Test simulation pour différents utilisateurs:");
        for (int i = 1; i <= 15; i++) {
            IPLocation loc = getLocationByIP(i);
            System.out.println("👤 User " + i + " -> " + loc.getCity() + " (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
        }
    }

    public static class IPLocation {
        private final double latitude;
        private final double longitude;
        private final String city;
        private final String country;
        private final String countryCode;
        private final String region;
        private final String isp;
        private final String zipCode;
        private final String source;

        public IPLocation(double latitude, double longitude, String city, String country,
                          String countryCode, String region, String isp, String zipCode, String source) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.city = city;
            this.country = country;
            this.countryCode = countryCode;
            this.region = region;
            this.isp = isp;
            this.zipCode = zipCode;
            this.source = source;
        }

        // Getters
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public String getCountryCode() { return countryCode; }
        public String getRegion() { return region; }
        public String getIsp() { return isp; }
        public String getZipCode() { return zipCode; }
        public String getSource() { return source; }

        public String getFormattedAddress() {
            return String.format("%s, %s, %s", city, region, country);
        }

        public String getPopupInfo() {
            return String.format(
                    "🌐 Localisation IP<br>" +
                            "📍 %s<br>" +
                            "🏙️ Ville: %s<br>" +
                            "🗺️ Région: %s<br>" +
                            "🇲🇦 Pays: %s<br>" +
                            "📡 FAI: %s<br>" +
                            "📮 Code postal: %s<br>" +
                            "🎯 Précision: Ville/Région<br>" +
                            "🔍 Source: %s",
                    getFormattedAddress(), city, region, country, isp, zipCode, source
            );
        }

        @Override
        public String toString() {
            return String.format("IPLocation{lat=%.6f, lon=%.6f, city=%s, country=%s, source=%s}",
                    latitude, longitude, city, country, source);
        }
    }

    // Méthode main pour tester
    public static void main(String[] args) {
        testSimulation();
    }
}