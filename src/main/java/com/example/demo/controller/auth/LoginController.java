package com.example.demo.controller.auth;

import com.example.demo.model.Database;
import com.example.demo.model.User;
import com.example.demo.view.auth.LoginView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

public abstract class LoginController {

    protected LoginView view;
    protected Stage stage;

    public LoginController(Stage stage) {
        this.stage = stage;
        this.view = new LoginView();
        stage.setScene(view.getScene());
        stage.setTitle("🔐 Connexion - Traffic Monitor Maroc");
        stage.centerOnScreen();
        stage.show();

        initActions();
    }

    private void initActions() {
        // Bouton Login
        view.getLoginBtn().setOnAction(e -> {
            handleLogin();
        });

        // Bouton Signup
        view.getSignupBtn().setOnAction(e -> {
            new SignupController(stage);
        });

        // Entrée sur le champ password
        view.getPasswordField().setOnAction(e -> {
            handleLogin();
        });

        // Animation au focus
        setupFieldAnimations();
    }

    private void handleLogin() {
        String email = view.getEmail().trim();
        String password = view.getPassword();

        // Validation des champs
        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Champs requis", "Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Email invalide", "Veuillez entrer une adresse email valide");
            return;
        }

        // Afficher loading
        view.getLoginBtn().setText("Connexion...");
        view.getLoginBtn().setDisable(true);

        // Simulation de délai pour UX
        new Thread(() -> {
            try {
                Thread.sleep(800); // Délai visuel

                Platform.runLater(() -> {
                    try {
                        if (Database.login(email, password)) {
                            User user = Database.getUserByEmail(email);
                            if (user != null) {
                                System.out.println("✅ Connexion réussie pour " + user.getUsername());

                                // Déclencher le callback de succès
                                onLoginSuccess(user.getId(), user.getEmail());
                            } else {
                                handleLoginError("Utilisateur non trouvé");
                            }
                        } else {
                            handleLoginError("Email ou mot de passe incorrect");
                        }
                    } catch (Exception e) {
                        handleLoginError("Erreur lors de la connexion à la base de données");
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    handleLoginError("Connexion interrompue");
                });
            }
        }).start();
    }

    private void handleLoginError(String errorMessage) {
        view.getLoginBtn().setText("Se connecter");
        view.getLoginBtn().setDisable(false);
        showAlert("Échec connexion", errorMessage);

        // Appeler la méthode onLoginFailure si elle existe
        try {
            onLoginFailure(errorMessage);
        } catch (AbstractMethodError e) {
            // La méthode n'est pas implémentée, c'est normal
            System.err.println("⚠️ onLoginFailure non implémenté: " + errorMessage);
        }
    }

    /**
     * Méthode de succès de connexion avec informations utilisateur
     */
    protected abstract void onLoginSuccess(int userId, String email);

    /**
     * Méthode optionnelle pour gérer les échecs de connexion
     * Fournit une implémentation par défaut pour éviter l'erreur de compilation
     */
    protected void onLoginFailure(String errorMessage) {
        // Implémentation par défaut vide
        System.err.println("❌ Échec connexion (défaut): " + errorMessage);
    }

    /**
     * Validation d'email simple
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Animations des champs de formulaire
     */
    private void setupFieldAnimations() {
        // Animation focus email
        view.getEmailField().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                view.getEmailField().setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
            } else {
                view.getEmailField().setStyle("-fx-border-color: #d1d5db; -fx-border-width: 1;");
            }
        });

        // Animation focus password
        view.getPasswordField().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                view.getPasswordField().setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
            } else {
                view.getPasswordField().setStyle("-fx-border-color: #d1d5db; -fx-border-width: 1;");
            }
        });
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Style personnalisé
            alert.getDialogPane().getStyleClass().add("error-alert");

            alert.showAndWait();
        });
    }

    /**
     * Afficher une confirmation
     */
    protected boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Réinitialiser le formulaire
     */
    public void resetForm() {
        Platform.runLater(() -> {
            view.getEmailField().clear();
            view.getPasswordField().clear();
            view.getLoginBtn().setText("Se connecter");
            view.getLoginBtn().setDisable(false);
        });
    }

    /**
     * Fermer le contrôleur
     */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    // Getters
    public LoginView getView() {
        return view;
    }

    public Stage getStage() {
        return stage;
    }
}