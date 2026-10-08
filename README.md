**Road Traffic Simulation**

A desktop application written in Java and JavaFX that simulates road traffic on top of an OpenStreetMap map. You can pick or draw a route, put vehicles on it, add traffic lights and incidents, and watch the simulation run. 

**What it does**

The main screen is an interactive map (Leaflet with OpenStreetMap tiles, displayed in a JavaFX WebView). From there you can compute routes between two points with OSRM, and open a simulation view for the selected route.

In the simulation, vehicles of different types (cars, trucks, buses) move along the route at their own speed. Traffic lights cycle between red, orange and green, and the map also supports stop and yield signs. Incidents can be reported at a location with a type and a severity. There is also a drawing mode where you trace your own itinerary and send a robot along it, next to the regular vehicles, with live statistics in the info panel.

The app can locate the user (IP-based geolocation, plus a small local server for more precise positions), and stores users and their vehicles in a MySQL database. There are login and signup screens, but the main class currently starts with `debugMode = true`, which skips the login and goes straight to the app.

**Built with**

Java 21, JavaFX 21, Maven, MySQL (with HikariCP for connection pooling), GeoTools and JTS for geographic data, Leaflet and OpenStreetMap for the map, OSRM for routing, JUnit 5 for tests.

**Project structure**

The code follows an MVC layout under `src/main/java/com/example/demo`: `model` holds the domain classes (routes, vehicles, traffic lights, incidents, users and the location services), `view` holds the JavaFX screens (map, simulation, route details, login), and `controller` connects them and runs the simulation loop.

**Running it**

You need Java 21, Maven and a local MySQL server.

1. Create a MySQL database 
2. Start the app:

```bash
git clone https://github.com/FATIMA-ZOHRA3/SIMJAVA.git
cd SIMJAVA
mvn javafx:run
```


**Author**

Fatima Zohra Bentouhami, [LinkedIn](https://www.linkedin.com/in/fatima-zohra-bentouhami-48b4a3309) 
