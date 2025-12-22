package com.example.demo.view.auth;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SignupView {
    private TextField emailField;
    private TextField usernameField; // ✅ AJOUT DU CHAMP USERNAME
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button signupBtn, backBtn;

    public Scene getScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Créer un compte");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");

        // ✅ AJOUT DU CHAMP USERNAME
        usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        emailField = new TextField();
        emailField.setPromptText("Email");

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");

        signupBtn = new Button("S'inscrire");
        backBtn = new Button("Retour");

        HBox buttons = new HBox(15, signupBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        // ✅ AJOUT DU USERNAME DANS L'INTERFACE
        root.getChildren().addAll(title, usernameField, emailField, passwordField, confirmPasswordField, buttons);
        root.setStyle("-fx-background-color: linear-gradient(to right, #4e54c8, #8f94fb);"
                + "-fx-padding: 50px; -fx-spacing: 20px;");

        styleButtons(signupBtn, backBtn);

        return new Scene(root, 450, 450); // ✅ AUGMENTÉ LA HAUTEUR
    }

    private void styleButtons(Button... buttons) {
        for(Button btn : buttons) {
            btn.setStyle("-fx-background-radius: 20px; -fx-font-size: 14px;"
                    + "-fx-padding: 10px 20px; -fx-background-color: white;"
                    + "-fx-text-fill: #4e54c8;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-radius: 20px;"
                    + "-fx-font-size: 14px; -fx-padding: 10px 20px;"
                    + "-fx-background-color: #e0e0ff; -fx-text-fill: #4e54c8;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-radius: 20px;"
                    + "-fx-font-size: 14px; -fx-padding: 10px 20px;"
                    + "-fx-background-color: white; -fx-text-fill: #4e54c8;"));
        }
    }

    // ✅ TOUS LES GETTERS NÉCESSAIRES
    public Button getSignupBtn() {
        return signupBtn;
    }

    public Button getBackBtn() {
        return backBtn;
    }

    public String getEmail() {
        return emailField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getUsername() { // ✅ MÉTHODE MANQUANTE
        return usernameField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordField != null ? confirmPasswordField.getText() : "";
    }

    public TextField getEmailField() {
        return emailField;
    }

    public TextField getUsernameField() { // ✅ GETTER POUR LE CHAMP
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public PasswordField getConfirmPasswordField() {
        return confirmPasswordField;
    }

    // ✅ MÉTHODE POUR EFFACER LES CHAMPS
    public void clearFields() {
        if (usernameField != null) usernameField.clear();
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }
}