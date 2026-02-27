package com.skilora.finance.controller;

import com.skilora.finance.model.Paiement;
import com.skilora.finance.service.PaiementService;
import com.skilora.finance.service.StripePaymentService;
import com.skilora.model.entity.User;
import com.skilora.model.service.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Contrôleur MVC pour paiement.fxml
 *
 * Règles :
 * - Validation champs (aucun vide, montant > 0)
 * - Task<Boolean> + Thread pour Stripe
 * - Sauvegarde DB (table paiement) uniquement si succès
 * - Nom Bénéficiaire = ComboBox chargé depuis la table users
 */
public class PaiementController {

    @FXML
    private TextField txtMontant;
    @FXML
    private TextField txtReferenceProjet;
    @FXML
    private ComboBox<String> cmbNomBeneficiaire; // ← ComboBox au lieu de TextField
    @FXML
    private TextField txtNumeroCarte;
    @FXML
    private TextField txtExpiration;
    @FXML
    private PasswordField txtCvv;
    @FXML
    private Button btnPayer;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label lblResultat;

    private final StripePaymentService stripePaymentService = new StripePaymentService();
    private final PaiementService paiementService = PaiementService.getInstance();

    // Gardé pour afficher dans la fenêtre succès
    private double dernierMontant = 0;
    private String dernierBeneficiaire = "";

    @FXML
    public void initialize() {
        appliquerFormatters();
        chargerBeneficiaires();
        lblResultat.setText("");
    }

    // ─────────────────────────────────────────────────────────────
    // Chargement des bénéficiaires depuis la BD
    // ─────────────────────────────────────────────────────────────

    private void chargerBeneficiaires() {
        try {
            UserService userService = UserService.getInstance();
            List<User> users = userService.getAllUsers();

            List<String> noms = users.stream()
                    .map(u -> {
                        String fn = u.getFullName();
                        return (fn != null && !fn.isBlank()) ? fn : u.getUsername();
                    })
                    .filter(n -> n != null && !n.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            cmbNomBeneficiaire.setItems(FXCollections.observableArrayList(noms));
        } catch (Exception e) {
            System.err.println("[PaiementController] Erreur chargement bénéficiaires: " + e.getMessage());
            // En cas d'erreur BD, laisser le ComboBox vide (l'utilisateur peut toujours
            // sélectionner)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Formatters saisie
    // ─────────────────────────────────────────────────────────────

    private void appliquerFormatters() {
        // Montant : chiffres + '.' ou ',' (max 2 décimales)
        UnaryOperator<TextFormatter.Change> montantFilter = change -> {
            String newText = change.getControlNewText();
            if (newText == null || newText.isEmpty())
                return change;
            // Autorise : 123 / 123.4 / 123,45
            if (newText.matches("\\d*(?:[.,]\\d{0,2})?"))
                return change;
            return null;
        };
        txtMontant.setTextFormatter(new TextFormatter<>(montantFilter));

        // Numéro carte : digits seulement, max 16, format XXXX XXXX XXXX XXXX
        txtNumeroCarte.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = digitsOnly(newVal);
            if (digits.length() > 16)
                digits = digits.substring(0, 16);
            String formatted = formatCardNumber(digits);
            if (!formatted.equals(newVal)) {
                int caret = txtNumeroCarte.getCaretPosition();
                txtNumeroCarte.setText(formatted);
                txtNumeroCarte.positionCaret(Math.min(formatted.length(), caret));
            }
        });

        // Expiration : MM/YY (auto slash)
        txtExpiration.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = digitsOnly(newVal);
            if (digits.length() > 4)
                digits = digits.substring(0, 4);
            String formatted;
            if (digits.length() <= 2)
                formatted = digits;
            else
                formatted = digits.substring(0, 2) + "/" + digits.substring(2);
            if (!formatted.equals(newVal)) {
                int caret = txtExpiration.getCaretPosition();
                txtExpiration.setText(formatted);
                txtExpiration.positionCaret(Math.min(formatted.length(), caret));
            }
        });

        // CVV : 3 chiffres max
        txtCvv.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = digitsOnly(newVal);
            if (digits.length() > 3)
                digits = digits.substring(0, 3);
            if (!digits.equals(newVal)) {
                txtCvv.setText(digits);
                txtCvv.positionCaret(digits.length());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Action Payer
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void onPayer() {
        // a) Validation
        String montantStr = safe(txtMontant.getText());
        String referenceProjet = safe(txtReferenceProjet.getText());
        String nomBeneficiaire = safe(cmbNomBeneficiaire.getValue()); // ← lecture ComboBox
        String numeroCarte = safe(txtNumeroCarte.getText());
        String expiration = safe(txtExpiration.getText());
        String cvv = safe(txtCvv.getText());

        if (montantStr.isEmpty() || referenceProjet.isEmpty() || nomBeneficiaire.isEmpty()
                || numeroCarte.isEmpty() || expiration.isEmpty() || cvv.isEmpty()) {
            afficherEchec("❌ Tous les champs sont obligatoires.");
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr.replace(",", "."));
        } catch (NumberFormatException e) {
            afficherEchec("❌ Montant invalide.");
            return;
        }

        if (montant <= 0) {
            afficherEchec("❌ Le montant doit être > 0.");
            return;
        }

        final String digitsCard = digitsOnly(numeroCarte);
        if (digitsCard.length() != 16) {
            afficherEchec("❌ Numéro de carte invalide (16 chiffres requis).");
            return;
        }

        if (!expiration.matches("\\d{2}/\\d{2}")) {
            afficherEchec("❌ Expiration invalide (MM/YY).");
            return;
        }

        final String mm = expiration.substring(0, 2);
        final String yy = expiration.substring(3, 5);
        int month = Integer.parseInt(mm);
        if (month < 1 || month > 12) {
            afficherEchec("❌ Mois d'expiration invalide.");
            return;
        }

        if (cvv.length() != 3) {
            afficherEchec("❌ CVV invalide (3 chiffres).");
            return;
        }

        // b) UI loading
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        btnPayer.setDisable(true);
        lblResultat.setText("Traitement du paiement en cours...");
        lblResultat.setStyle("-fx-text-fill: -fx-muted-foreground;");

        final long montantCentimes = Math.round(montant * 100);
        final String finalMm = mm;
        final String finalExpYear = "20" + yy; // 12/26 → 2026
        final String finalCvv = cvv;

        // Mémoriser pour la fenêtre de succès
        dernierMontant = montant;
        dernierBeneficiaire = nomBeneficiaire;

        // c) Task + Thread — flux Stripe corrigé :
        // 1. resolvePaymentMethodId (pm_card_visa pour cartes test connues)
        // 2. createPaymentIntent
        // 3. confirmPayment(pi, pm)
        Task<PaiementResult> task = new Task<>() {
            @Override
            protected PaiementResult call() {
                try {
                    // Étape 1 : Obtenir le PM ID (prédéfini test OU créé via API)
                    String pmId = stripePaymentService.resolvePaymentMethodId(
                            digitsCard, finalMm, finalExpYear, finalCvv);

                    // Étape 2 : Créer le PaymentIntent
                    String intentId = stripePaymentService.createPaymentIntent(montantCentimes);

                    // Étape 3 : Confirmer sans numéros bruts
                    boolean success = stripePaymentService.confirmPayment(intentId, pmId);

                    if (success) {
                        Paiement p = new Paiement();
                        p.setMontant(montant);
                        p.setDateHeure(LocalDateTime.now());
                        p.setStatut(Paiement.Statut.SUCCESS);
                        p.setStripePaymentId(intentId);
                        p.setReferenceProjet(referenceProjet);
                        p.setNomBeneficiaire(nomBeneficiaire);
                        boolean saved = paiementService.ajouterPaiement(p);

                        // Envoi SMS de confirmation
                        try {
                            com.skilora.finance.service.SmsService.getInstance()
                                    .sendPaymentSuccess(montant, nomBeneficiaire, referenceProjet, intentId);
                        } catch (Exception smsEx) {
                            System.err.println("[SMS] Envoi échoué (paiement tjs validé): " + smsEx.getMessage());
                        }

                        return new PaiementResult(true, intentId, saved);
                    }
                    return new PaiementResult(false, null, false);
                } catch (Exception e) {
                    return new PaiementResult(false, null, false, e.getMessage());
                }
            }
        };

        task.setOnSucceeded(evt -> {
            PaiementResult r = task.getValue();
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);
                btnPayer.setDisable(false);
                if (r != null && r.success) {
                    afficherSucces("✅ Paiement réussi ! ID: " + r.stripePaymentId);
                    showSuccessDialog(r.stripePaymentId);
                } else {
                    String msg = "❌ Paiement échoué. Vérifiez vos informations.";
                    if (r != null && r.errorMessage != null && !r.errorMessage.isBlank()) {
                        msg = msg + " (" + r.errorMessage + ")";
                    }
                    afficherEchec(msg);
                }
            });
        });

        task.setOnFailed(evt -> Platform.runLater(() -> {
            afficherEchec("❌ Paiement échoué. Erreur technique.");
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            btnPayer.setDisable(false);
        }));

        Thread t = new Thread(task, "stripe-paiement-thread");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────

    private void afficherSucces(String message) {
        lblResultat.setText(message);
        lblResultat.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void afficherEchec(String message) {
        lblResultat.setText(message);
        lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    /**
     * Affiche une fenêtre de confirmation visuelle après un paiement réussi.
     */
    private void showSuccessDialog(String transactionId) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Paiement Confirmé");
        dialog.setHeaderText(null);

        // Contenu de la fenêtre
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(18);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(28, 36, 20, 36));
        content.setStyle("-fx-background-color: #0f172a;");
        content.setPrefWidth(420);

        // Icone succès
        javafx.scene.control.Label iconLabel = new javafx.scene.control.Label("✅");
        iconLabel.setStyle("-fx-font-size: 52px;");

        // Titre
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Paiement Réussi !");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #22c55e;");

        // Séparateur
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color: #334155;");

        // Détails dans une grille
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        String labelStyle = "-fx-text-fill: #94a3b8; -fx-font-size: 13px;";
        String valueStyle = "-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;";

        // Montant
        addGridRow(grid, 0, "💰  Montant payé",
                String.format("%.2f USD", dernierMontant), labelStyle, valueStyle);

        // Bénéficiaire
        addGridRow(grid, 1, "👤  Bénéficiaire",
                dernierBeneficiaire, labelStyle, valueStyle);

        // Date
        String dateStr = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
        addGridRow(grid, 2, "📅  Date", dateStr, labelStyle, valueStyle);

        // ID Transaction
        javafx.scene.control.Label idKey = new javafx.scene.control.Label("🔑  ID Transaction");
        idKey.setStyle(labelStyle);
        javafx.scene.control.Label idVal = new javafx.scene.control.Label(
                transactionId != null ? transactionId : "—");
        idVal.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
        idVal.setWrapText(true);
        idVal.setMaxWidth(220);
        grid.add(idKey, 0, 3);
        grid.add(idVal, 1, 3);

        // Note Stripe
        javafx.scene.control.Label noteLabel = new javafx.scene.control.Label(
                "Paiement traité via Stripe (mode TEST)");
        noteLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-style: italic;");

        // Bouton fermer
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("✓  Fermer");
        btnClose.setStyle(
                "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 14px; -fx-padding: 10 32; -fx-background-radius: 10; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 14px; -fx-padding: 10 32; -fx-background-radius: 10; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 14px; -fx-padding: 10 32; -fx-background-radius: 10; -fx-cursor: hand;"));

        content.getChildren().addAll(iconLabel, titleLabel, sep, grid, noteLabel, btnClose);

        // Appliquer le style sombre à la DialogPane
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CLOSE).setVisible(false);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a; -fx-padding: 0;");

        // Réinitialiser le formulaire après fermeture
        dialog.setOnHidden(e -> resetForm());

        dialog.showAndWait();
    }

    private void addGridRow(javafx.scene.layout.GridPane grid, int row,
            String key, String value, String keyStyle, String valStyle) {
        javafx.scene.control.Label k = new javafx.scene.control.Label(key);
        k.setStyle(keyStyle);
        javafx.scene.control.Label v = new javafx.scene.control.Label(value);
        v.setStyle(valStyle);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    /**
     * Remet le formulaire à zéro après un paiement réussi.
     */
    private void resetForm() {
        txtMontant.clear();
        txtReferenceProjet.clear();
        cmbNomBeneficiaire.setValue(null);
        txtNumeroCarte.clear();
        txtExpiration.clear();
        txtCvv.clear();
        lblResultat.setText("");
    }

    // ─────────────────────────────────────────────────────────────
    // Static helpers
    // ─────────────────────────────────────────────────────────────

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static String formatCardNumber(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0)
                sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Inner class résultat paiement
    // ─────────────────────────────────────────────────────────────

    private static class PaiementResult {
        final boolean success;
        final String stripePaymentId;
        final boolean savedInDb;
        final String errorMessage;

        PaiementResult(boolean success, String stripePaymentId, boolean savedInDb) {
            this(success, stripePaymentId, savedInDb, null);
        }

        PaiementResult(boolean success, String stripePaymentId, boolean savedInDb, String errorMessage) {
            this.success = success;
            this.stripePaymentId = stripePaymentId;
            this.savedInDb = savedInDb;
            this.errorMessage = errorMessage;
        }
    }
}
