package com.example.demo.controller.auth;

import com.example.demo.model.Database;
import com.example.demo.model.User;
import com.example.demo.view.auth.SignupView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class SignupController {

    private SignupView view;
    private Stage stage;

    public SignupController(Stage stage) {
        this.stage = stage;
        this.view = new SignupView();
        stage.setScene(view.getScene());
        stage.setTitle("📝 Inscription - Traffic Monitor Maroc");
        stage.centerOnScreen();
        stage.show(); // ✅ AJOUT: Afficher la fenêtre

        initActions();
    }

    private void initActions() {
        // Bouton S'inscrire
        view.getSignupBtn().setOnAction(e -> {
            handleSignup();
        });

        // Bouton Retour
        view.getBackBtn().setOnAction(e -> {
            goBackToLogin();
        });

        // Entrée sur confirmation password
        view.getConfirmPasswordField().setOnAction(e -> {
            handleSignup();
        });
    }

    private void handleSignup() {
        String username = view.getUsername().trim();
        String email = view.getEmail().trim();
        String password = view.getPassword();
        String confirmPassword = view.getConfirmPassword();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Champs requis", "Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Email invalide", "Veuillez entrer une adresse email valide");
            return;
        }

        if (password.length() < 6) {
            showAlert("Mot de passe faible", "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Mots de passe différents", "Les mots de passe ne correspondent pas");
            return;
        }

        // Afficher loading
        view.getSignupBtn().setText("Inscription...");
        view.getSignupBtn().setDisable(true);

        new Thread(() -> {
            try {
                Thread.sleep(800); // Délai visuel

                Platform.runLater(() -> {
                    User newUser = new User(username, email, password);

                    if (Database.register(newUser)) {
                        showSuccessAndReturn();
                    } else {
                        view.getSignupBtn().setText("S'inscrire");
                        view.getSignupBtn().setDisable(false);
                        showAlert("Email existant", "Cet email est déjà utilisé");
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    view.getSignupBtn().setText("S'inscrire");
                    view.getSignupBtn().setDisable(false);
                    showAlert("Erreur", "Erreur lors de l'inscription");
                });
            }
        }).start();
    }

    private void showSuccessAndReturn() {
        view.getSignupBtn().setText("✅ Inscription réussie!");

        new Thread(() -> {
            try {
                Thread.sleep(1500);

                Platform.runLater(() -> {
                    goBackToLogin();
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(this::goBackToLogin);
            }
        }).start();
    }

    private void goBackToLogin() {
        // ✅ CORRECTION: Implémentation correcte de la méthode abstraite
        new LoginController(stage) {
            @Override
            protected void onLoginSuccess(int userId, String email) {
                // Cette méthode sera redéfinie dans Main.java
                // Pour l'inscription, on retourne simplement au login
                System.out.println("Retour à l'écran de connexion après inscription");
            }
        };
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}