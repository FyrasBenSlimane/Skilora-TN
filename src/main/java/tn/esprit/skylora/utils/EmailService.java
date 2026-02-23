package tn.esprit.skylora.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Service d'envoi d'e-mails via le protocole SMTP de Gmail.
 * Utilisé pour envoyer des notifications automatiques aux utilisateurs
 * lorsque le statut de leurs tickets est modifié par un administrateur.
 *
 * Configuration :
 * - Serveur SMTP : smtp.gmail.com (port 587, STARTTLS)
 * - Expéditeur : hassanjebri99@gmail.com
 * - Destinataire par défaut : hsan.jebri@esprit.tn
 * - Authentification via mot de passe d'application Gmail
 */
public class EmailService {

    /** Adresse e-mail de l'expéditeur (compte Gmail de l'application) */
    private static final String FROM_EMAIL = "bouchaddakhraed@gmail.com";

    /**
     * Mot de passe d'application Gmail (généré dans les paramètres de sécurité
     * Google)
     */
    private static final String APP_PASSWORD = "erhgfxblqxkucqii";

    /** Adresse e-mail du destinataire des notifications */
    private static final String TO_EMAIL = "raed.Bouchaddakh@esprit.tn";

    /**
     * Envoie un e-mail de notification avec le sujet et le contenu spécifiés.
     * Utilise SMTP avec STARTTLS pour une connexion sécurisée.
     * En cas d'erreur (réseau, authentification, etc.), l'exception est loggée dans
     * la console.
     *
     * @param subject Le sujet de l'e-mail (ex: "Mise à jour du Ticket #5")
     * @param content Le corps textuel de l'e-mail à envoyer
     */
    public static void sendEmail(String subject, String content) {
        // Configuration des propriétés SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Création de la session avec authentification
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            // Construction et envoi du message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            System.out.println("Email envoyé avec succès !");
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
