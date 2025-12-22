package com.example.demo.model;

public class FeuCirculation {
    private String id;
    private double latitude;
    private double longitude;
    private String type; // "feu_rouge", "feu_orange", "feu_vert", "stop", "cedez"
    private String etat; // "rouge", "orange", "vert"
    private int dureeCycle; // Durée totale du cycle en secondes
    private long tempsDebutCycle; // Temps du début du cycle
    private String routeId;
    private String nom;
    private int tempsRouge = 30; // Durée rouge par défaut
    private int tempsVert = 25; // Durée vert par défaut
    private int tempsOrange = 5; // Durée orange par défaut
    private int compteurTemps = 0; // Compteur pour le timing
    private int delai = 0; // Délai avant changement d'état

    public FeuCirculation(String id, double latitude, double longitude, String type, String routeId, String nom) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.routeId = routeId;
        this.nom = nom;
        this.etat = "rouge"; // État initial
        this.dureeCycle = 30; // Cycle de 30 secondes par défaut
        this.tempsDebutCycle = System.currentTimeMillis();

        System.out.println("🚦 Feu de circulation créé: " + nom + " (" + type + ") à " + latitude + ", " + longitude);
    }

    // Getters
    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public String getEtat() { return etat; }
    public String getRouteId() { return routeId; }
    public String getNom() { return nom; }
    public int getDureeCycle() { return dureeCycle; }

    // Mettre à jour l'état du feu en fonction du temps
    public void mettreAJourEtat() {
        long tempsEcoule = (System.currentTimeMillis() - tempsDebutCycle) / 1000;
        int positionDansCycle = (int) (tempsEcoule % dureeCycle);
        
        if (positionDansCycle < dureeCycle * 0.4) { // 40% du cycle = rouge
            this.etat = "rouge";
        } else if (positionDansCycle < dureeCycle * 0.5) { // 10% du cycle = orange
            this.etat = "orange";
        } else { // 50% du cycle = vert
            this.etat = "vert";
        }
    }

    // Vérifier si le feu est rouge
    public boolean estRouge() {
        return "rouge".equals(etat);
    }

    // Vérifier si le feu est vert
    public boolean estVert() {
        return "vert".equals(etat);
    }

    // Changer manuellement l'état
    public void setEtat(String nouvelEtat) {
        this.etat = nouvelEtat;
    }

    // Setter for nom
    public void setNom(String nom) {
        this.nom = nom;
    }

    // Redémarrer le cycle
    public void redemarrerCycle() {
        this.tempsDebutCycle = System.currentTimeMillis();
    }

    // Setters pour les temps
    public void setTempsRouge(int tempsRouge) {
        this.tempsRouge = tempsRouge;
        this.dureeCycle = this.tempsRouge + this.tempsVert + this.tempsOrange;
    }

    public void setTempsVert(int tempsVert) {
        this.tempsVert = tempsVert;
        this.dureeCycle = this.tempsRouge + this.tempsVert + this.tempsOrange;
    }

    public void setTempsOrange(int tempsOrange) {
        this.tempsOrange = tempsOrange;
        this.dureeCycle = this.tempsRouge + this.tempsVert + this.tempsOrange;
    }

    public void setDelai(int delai) {
        this.delai = delai;
    }

    // Getters pour les temps
    public int getTempsRouge() {
        return tempsRouge;
    }

    public int getTempsVert() {
        return tempsVert;
    }

    public int getTempsOrange() {
        return tempsOrange;
    }

    public int getDelai() {
        return delai;
    }

    // Gestion du compteur de temps
    public int getCompteurTemps() {
        return compteurTemps;
    }

    public void setCompteurTemps(int compteurTemps) {
        this.compteurTemps = compteurTemps;
    }

    public void decrementerCompteur() {
        if (compteurTemps > 0) {
            compteurTemps--;
        }
    }

    @Override
    public String toString() {
        return "FeuCirculation{" +
                "id='" + id + '\'' +
                ", nom='" + nom + '\'' +
                ", type='" + type + '\'' +
                ", etat='" + etat + '\'' +
                ", position=" + latitude + ", " + longitude +
                '}';
    }
}