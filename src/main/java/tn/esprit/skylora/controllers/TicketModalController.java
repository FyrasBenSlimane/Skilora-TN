package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TicketModalController implements Initializable {

    @FXML
    private Label modalTitle;
    @FXML
    private TextField subjectField;
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private ComboBox<String> priorityBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private ServiceTicket serviceTicket = new ServiceTicket();
    private UserDashboardController parentController;
    private Ticket existingTicket;

    // Liste des mots interdits (insultes, vulgarités, etc.)
    private static final List<String> BAD_WORDS = Arrays.asList(
            // Insultes courantes en français
            "connard", "salaud", "salope", "putain", "merde", "con", "conne",
            "enculé", "enculer", "bordel", "chier", "bite", "couille", "foutre",
            "pute", "pétasse", "garce", "bâtard", "crétin", "abruti", "imbécile",
            "débile", "idiot", "nul", "minable", "pourri", "ordure", "salopard",

            // Insultes en anglais
            "fuck", "shit", "bitch", "asshole", "bastard", "damn", "crap",
            "dick", "pussy", "cock", "motherfucker", "nigger", "fag", "whore",

            // Insultes en arabe (translittéré)
            "kalbek", "kalb", "hmar", "khanzir", "kahba", "7mar", "5anzir",

            // Variantes et abréviations
            "wtf", "stfu", "gtfo", "fck", "sht", "btch", "fk"
    );

    // Longueurs minimales et maximales
    private static final int MIN_SUBJECT_LENGTH = 5;
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final int MIN_DESCRIPTION_LENGTH = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    @Override
    // Initialise les listes déroulantes (catégorie, priorité)
    public void initialize(URL url, ResourceBundle rb) {
        categoryBox.getItems().addAll("TECHNICAL", "PAYMENT", "ACCOUNT", "FORMATION", "RECRUITMENT", "OTHER");
        priorityBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT");

        // Ajouter des listeners pour la validation en temps réel
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > MAX_SUBJECT_LENGTH) {
                subjectField.setText(oldVal);
            }
        });

        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > MAX_DESCRIPTION_LENGTH) {
                descriptionArea.setText(oldVal);
            }
        });
    }

    // Garde une référence vers le tableau de bord pour pouvoir le rafraîchir
    public void setParentController(UserDashboardController parentController) {
        this.parentController = parentController;
    }

    // Remplit le formulaire avec les données d'un ticket existant (si modification)
    public void setTicketData(Ticket ticket) {
        this.existingTicket = ticket;
        modalTitle.setText("Modifier le Ticket #" + ticket.getId());
        subjectField.setText(ticket.getSubject());
        categoryBox.setValue(ticket.getCategorie());
        priorityBox.setValue(ticket.getPriorite());
        descriptionArea.setText(ticket.getDescription());
    }

    @FXML
    // Sauvegarde le ticket (création ou modification) après vérification des champs
    private void handleSave() {
        // Réinitialiser le message d'erreur
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Validation des champs obligatoires
        String validationError = validateInputs();
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            if (existingTicket == null) {
                // Nouveau ticket
                Ticket newTicket = new Ticket(1, // Simulation user 1
                        subjectField.getText().trim(),
                        categoryBox.getValue(),
                        priorityBox.getValue(),
                        "OUVERT",
                        descriptionArea.getText().trim());
                serviceTicket.ajouter(newTicket);
            } else {
                // Modification
                existingTicket.setSubject(subjectField.getText().trim());
                existingTicket.setCategorie(categoryBox.getValue());
                existingTicket.setPriorite(priorityBox.getValue());
                existingTicket.setDescription(descriptionArea.getText().trim());
                serviceTicket.modifier(existingTicket);
            }

            parentController.loadTickets();
            close();
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Valide tous les champs de saisie et retourne un message d'erreur si nécessaire
    private String validateInputs() {
        String subject = subjectField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = categoryBox.getValue();
        String priority = priorityBox.getValue();

        // Vérifier les champs vides
        if (subject.isEmpty()) {
            return "Le sujet ne peut pas être vide.";
        }
        if (description.isEmpty()) {
            return "La description ne peut pas être vide.";
        }
        if (category == null) {
            return "Veuillez sélectionner une catégorie.";
        }
        if (priority == null) {
            return "Veuillez sélectionner une priorité.";
        }

        // Vérifier la longueur minimale du sujet
        if (subject.length() < MIN_SUBJECT_LENGTH) {
            return "Le sujet doit contenir au moins " + MIN_SUBJECT_LENGTH + " caractères.";
        }

        // Vérifier la longueur minimale de la description
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            return "La description doit contenir au moins " + MIN_DESCRIPTION_LENGTH + " caractères.";
        }

        // Vérifier que le sujet n'est pas uniquement des chiffres ou des symboles
        if (!containsLetters(subject)) {
            return "Le sujet doit contenir au moins quelques lettres.";
        }

        // Vérifier que la description n'est pas uniquement des chiffres ou des symboles
        if (!containsLetters(description)) {
            return "La description doit contenir au moins quelques lettres.";
        }

        // Vérifier la présence de mots interdits dans le sujet
        String badWordInSubject = containsBadWord(subject);
        if (badWordInSubject != null) {
            return "Le sujet contient un langage inapproprié: \"" + badWordInSubject + "\". Veuillez reformuler.";
        }

        // Vérifier la présence de mots interdits dans la description
        String badWordInDescription = containsBadWord(description);
        if (badWordInDescription != null) {
            return "La description contient un langage inapproprié: \"" + badWordInDescription + "\". Veuillez reformuler.";
        }

        // Vérifier que le texte ne contient pas de spam (répétition excessive)
        if (isSpam(subject) || isSpam(description)) {
            return "Votre texte contient trop de répétitions. Veuillez fournir un contenu plus varié.";
        }

        return null; // Tout est valide
    }

    // Vérifie si le texte contient au moins quelques lettres
    private boolean containsLetters(String text) {
        return text.matches(".*[a-zA-Zà-ÿÀ-Ÿ]+.*");
    }

    // Vérifie si le texte contient un mot interdit et retourne le mot trouvé
    private String containsBadWord(String text) {
        String lowerText = text.toLowerCase();

        // Remplacer les caractères spéciaux souvent utilisés pour contourner les filtres
        lowerText = lowerText.replaceAll("[0@]", "o")
                .replaceAll("[1!|]", "i")
                .replaceAll("3", "e")
                .replaceAll("4", "a")
                .replaceAll("5", "s")
                .replaceAll("7", "t")
                .replaceAll("8", "b")
                .replaceAll("[\\$]", "s")
                .replaceAll("[*]", "");

        for (String badWord : BAD_WORDS) {
            // Créer un pattern qui ignore les espaces et caractères spéciaux entre les lettres
            String pattern = badWord.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .reduce("", (s1, s2) -> s1 + s2 + "[\\s\\-_\\.]*");

            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            if (p.matcher(lowerText).find()) {
                return badWord;
            }

            // Vérification simple pour les mots exacts
            if (lowerText.contains(badWord)) {
                return badWord;
            }
        }
        return null;
    }

    // Détecte si le texte est du spam (trop de répétitions)
    private boolean isSpam(String text) {
        if (text.length() < 10) return false;

        // Vérifier les répétitions de caractères (ex: "aaaaaaaa")
        if (text.matches(".*(.)\\1{7,}.*")) {
            return true;
        }

        // Vérifier les répétitions de mots courts
        String[] words = text.split("\\s+");
        if (words.length >= 5) {
            int repeatCount = 0;
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equalsIgnoreCase(words[i + 1]) && words[i].length() > 2) {
                    repeatCount++;
                    if (repeatCount >= 3) return true;
                } else {
                    repeatCount = 0;
                }
            }
        }

        return false;
    }

    // Affiche un message d'erreur
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    // Ferme la fenêtre sans enregistrer
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}