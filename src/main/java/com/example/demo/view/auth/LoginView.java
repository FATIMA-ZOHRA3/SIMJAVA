package com.example.demo.view.auth;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView {
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginBtn, signupBtn;

    public Scene getScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Connexion");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");

        emailField = new TextField();
        emailField.setPromptText("Email");

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        loginBtn = new Button("Se connecter");
        signupBtn = new Button("Créer un compte");

        HBox buttons = new HBox(15, loginBtn, signupBtn);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, emailField, passwordField, buttons);
        root.setStyle("-fx-background-color: linear-gradient(to right, #4e54c8, #8f94fb);"
                + "-fx-padding: 50px; -fx-spacing: 20px;");

        styleButtons(loginBtn, signupBtn);

        return new Scene(root, 450, 350);
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

    // Getters existants
    public Button getLoginBtn() { return loginBtn; }
    public Button getSignupBtn() { return signupBtn; }
    public String getEmail() { return emailField.getText(); }
    public String getPassword() { return passwordField.getText(); }

    // ✅ AJOUT DES GETTERS MANQUANTS POUR LES CHAMPS
    public TextField getEmailField() {
        return emailField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }
}