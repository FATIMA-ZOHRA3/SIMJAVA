package com.example.demo.model;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeolocationServer {
    private final HttpServer server;
    private final int port;
    private BiConsumer<Double, Double> locationConsumer;

    public GeolocationServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", new GeolocationHandler());
    }

    public void start() {
        server.start();
        System.out.println("✅ Serveur de géolocalisation démarré sur le port " + port);
        
        // Ouvrir le navigateur automatiquement
        openBrowser();
    }

    public void stop() {
        server.stop(0);
        System.out.println("❌ Serveur de géolocalisation arrêté");
    }

    public void setLocationConsumer(BiConsumer<Double, Double> consumer) {
        this.locationConsumer = consumer;
    }

    private void openBrowser() {
        try {
            String url = "http://localhost:" + port;
            Desktop.getDesktop().browse(new URI(url));
            System.out.println("🌐 Navigateur ouvert sur: " + url);
        } catch (Exception e) {
            System.err.println("❌ Impossible d'ouvrir le navigateur: " + e.getMessage());
        }
    }

    private class GeolocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(requestMethod) && "/".equals(path)) {
                serveHTMLPage(exchange);
            } else if ("POST".equals(requestMethod) && "/location".equals(path)) {
                handleLocationPost(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        }

        private void serveHTMLPage(HttpExchange exchange) throws IOException {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Géolocalisation</title>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            max-width: 800px;
                            margin: 0 auto;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            min-height: 100vh;
                        }
                        .container {
                            background: rgba(255,255,255,0.1);
                            padding: 30px;
                            border-radius: 15px;
                            backdrop-filter: blur(10px);
                        }
                        button {
                            background: #4CAF50;
                            color: white;
                            border: none;
                            padding: 15px 30px;
                            border-radius: 8px;
                            cursor: pointer;
                            font-size: 16px;
                            margin: 10px;
                        }
                        button:hover {
                            background: #45a049;
                        }
                        #coordinates {
                            margin-top: 20px;
                            padding: 15px;
                            background: rgba(0,0,0,0.3);
                            border-radius: 8px;
                            display: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🌍 Service de Géolocalisation</h1>
                        <p>Cliquez pour partager votre position :</p>
                        <button onclick="getLocation()">📍 Obtenir ma position</button>
                        <div id="coordinates">
                            <h3>Coordonnées obtenues :</h3>
                            <p id="coords"></p>
                            <button onclick="sendToApp()">📨 Envoyer à l'application</button>
                        </div>
                    </div>

                    <script>
                        function getLocation() {
                            if (navigator.geolocation) {
                                navigator.geolocation.getCurrentPosition(
                                    showPosition,
                                    showError,
                                    {
                                        enableHighAccuracy: true,
                                        timeout: 10000,
                                        maximumAge: 0
                                    }
                                );
                            } else {
                                alert("La géolocalisation n'est pas supportée par ce navigateur.");
                            }
                        }

                        function showPosition(position) {
                            const coords = position.coords;
                            const coordsText = `Latitude: ${coords.latitude}<br>Longitude: ${coords.longitude}<br>Précision: ${coords.accuracy}m`;
                            
                            document.getElementById('coords').innerHTML = coordsText;
                            document.getElementById('coordinates').style.display = 'block';
                            
                            // Stocker les coordonnées pour l'envoi
                            window.currentCoords = {
                                lat: coords.latitude,
                                lon: coords.longitude
                            };
                        }

                        function showError(error) {
                            let message = "Erreur inconnue";
                            switch(error.code) {
                                case error.PERMISSION_DENIED:
                                    message = "Permission refusée. Autorisez la géolocalisation.";
                                    break;
                                case error.POSITION_UNAVAILABLE:
                                    message = "Position indisponible.";
                                    break;
                                case error.TIMEOUT:
                                    message = "Délai dépassé.";
                                    break;
                            }
                            alert("Erreur: " + message);
                        }

                        function sendToApp() {
                            if (window.currentCoords) {
                                fetch('/location', {
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/json',
                                    },
                                    body: JSON.stringify(window.currentCoords)
                                })
                                .then(response => response.text())
                                .then(data => {
                                    alert('✅ Position envoyée à l\\'application!');
                                })
                                .catch(error => {
                                    alert('❌ Erreur envoi: ' + error);
                                });
                            }
                        }

                        // Essayer automatiquement après 2 secondes
                        setTimeout(() => {
                            document.querySelector('button').click();
                        }, 2000);
                    </script>
                </body>
                </html>
                """;

            sendResponse(exchange, 200, html);
        }

        private void handleLocationPost(HttpExchange exchange) throws IOException {
            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }

                // Extraire les coordonnées du JSON
                String json = requestBody.toString();
                Pattern latPattern = Pattern.compile("\"lat\":\\s*([0-9.]+)");
                Pattern lonPattern = Pattern.compile("\"lon\":\\s*([0-9.-]+)");
                
                Matcher latMatcher = latPattern.matcher(json);
                Matcher lonMatcher = lonPattern.matcher(json);

                if (latMatcher.find() && lonMatcher.find()) {
                    double lat = Double.parseDouble(latMatcher.group(1));
                    double lon = Double.parseDouble(lonMatcher.group(1));

                    System.out.println("📍 Coordonnées reçues: " + lat + ", " + lon);

                    // Notifier le consommateur
                    if (locationConsumer != null) {
                        locationConsumer.accept(lat, lon);
                    }

                    sendResponse(exchange, 200, "Coordonnées reçues: " + lat + ", " + lon);
                } else {
                    sendResponse(exchange, 400, "Format de coordonnées invalide");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Erreur interne: " + e.getMessage());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}