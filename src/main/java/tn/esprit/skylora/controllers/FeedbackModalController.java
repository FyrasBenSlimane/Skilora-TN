package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.Rating;
import tn.esprit.skylora.entities.Feedback;
import tn.esprit.skylora.services.ServiceFeedback;

import java.sql.SQLException;

/**
 * Contrôleur de la fenêtre modale de dépôt de feedback.
 * Permet à l'utilisateur de noter un ticket résolu (de 1 à 5 étoiles)
 * et de laisser un commentaire. Inclut une détection de mots inappropriés
 * et une analyse de sentiment via l'IA Gemini.
 */
public class FeedbackModalController {

    @FXML
    private Rating ratingField;
    @FXML
    private TextArea commentArea;

    /** Identifiant du ticket associé à ce feedback */
    private int ticketId;
    private ServiceFeedback serviceFeedback = new ServiceFeedback();

    /**
     * Définit l'identifiant du ticket pour lequel le feedback est donné.
     *
     * @param ticketId L'ID du ticket concerné
     */
    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    /**
     * Soumet le feedback de l'utilisateur.
     * Effectue dans l'ordre :
     * 1. Vérification des mots inappropriés dans le commentaire
     * 2. Validation que une note est bien sélectionnée
     * 3. Analyse de sentiment via l'IA Gemini
     * 4. Enregistrement du feedback en base de données
     * 5. Affichage d'une notification de confirmation
     */
    @FXML
    private void handleSubmit() {
        int rating = (int) ratingField.getRating();
        String comment = commentArea.getText().toLowerCase();

        // 1. Vérification des mots inappropriés
        String[] badWords = { "merde", "con", "putain", "saloperie", "fuck", "shit" };
        boolean hasBadWord = false;
        for (String word : badWords) {
            if (comment.contains(word)) {
                hasBadWord = true;
                break;
            }
        }

        if (hasBadWord) {
            applyPunishment();
            return;
        }

        if (rating == 0) {
            Notifications.create()
                    .title("Note requise")
                    .text("S'il vous plaît, sélectionnez une note avant d'envoyer.")
                    .showWarning();
            return;
        }

        try {
            // Sentiment Analysis
            String sentiment = tn.esprit.skylora.utils.GeminiService.analyzeSentiment(comment);

            Feedback f = new Feedback(ticketId, rating, comment);
            serviceFeedback.ajouter(f);

            Notifications.create()
                    .title("Merci ! (Sentiment: " + sentiment + ")")
                    .text("Votre feedback a été enregistré avec succès. Note d'analyse: " + sentiment)
                    .showInformation();

            closeModal();
        } catch (SQLException e) {
            e.printStackTrace();
            Notifications.create()
                    .title("Erreur")
                    .text("Impossible d'enregistrer le feedback. Vérifiez si la table 'feedback' existe !")
                    .showError();
        }
    }

    /**
     * Compteur de tentatives avec langage inapproprié. Déclenche une sanction
     * croissante.
     */
    private int strikeCount = 0;

    /**
     * Applique une sanction à l'utilisateur ayant utilisé un langage inapproprié.
     * - 1er avertissement : notification simple
     * - Avertissements suivants : multiples notifications aléatoires envahissantes
     */
    private void applyPunishment() {
        strikeCount++;
        if (strikeCount == 1) {
            // Level 1: Short warning
            Notifications.create()
                    .title("Attention !")
                    .text("Langage inapproprié détecté. Soyez poli !")
                    .hideAfter(javafx.util.Duration.seconds(10))
                    .showWarning();
        } else {
            // Level 2+: THE DISTURBANCE
            Notifications.create()
                    .title("ESPÈCE DE MALPOLI !")
                    .text("Tu as voulu jouer ? Tu vas payer ! Hahaha !")
                    .hideAfter(javafx.util.Duration.seconds(60))
                    .showError();

            // Spawn multiple spinning notifications to "disturb" him
            for (int i = 0; i < 5; i++) {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(i * 500));
                pause.setOnFinished(e -> {
                    Notifications.create()
                            .title("RÉFLÉCHIS À TES ACTES")
                            .text("C'est mal de dire des gros mots...")
                            .position(org.controlsfx.control.action.ActionUtils.ACTION_SPAN.equals("") ? null
                                    : javafx.geometry.Pos.values()[(int) (Math.random() * 9)])
                            .hideAfter(javafx.util.Duration.seconds(30))
                            .showWarning();
                });
                pause.play();
            }
        }
    }

    /**
     * Ferme la fenêtre modale sans enregistrer le feedback.
     */
    @FXML
    private void handleCancel() {
        closeModal();
    }

    /**
     * Ferme la fenêtre modale.
     */
    private void closeModal() {
        Stage stage = (Stage) ratingField.getScene().getWindow();
        stage.close();
    }
}
