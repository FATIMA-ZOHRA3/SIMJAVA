package com.example.demo.model;

import java.sql.*;

public class Database {

    private static final String URL =
            "jdbc:mysql://localhost:3306/trafficsimulation";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ Driver MySQL chargé.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ Impossible de charger le driver MySQL !", e);
        }
    }

    // Connexion à la base
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // ----------------- Gestion des utilisateurs -----------------

    // Inscription
    public static boolean register(User user) {
        String checkQuery = "SELECT * FROM users WHERE email = ?";
        String insertQuery = "INSERT INTO users(username, email, password) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, user.getEmail());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // Email déjà utilisé
                return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, user.getUsername());
                insertStmt.setString(2, user.getEmail());
                insertStmt.setString(3, user.getPassword()); // ⚠️ Hasher pour vrai projet
                insertStmt.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Connexion
    public static boolean login(String email, String password) {
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // true si utilisateur trouvé

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Récupérer un utilisateur
    public static User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
