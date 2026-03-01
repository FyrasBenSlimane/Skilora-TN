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
     * Génère un template HTML professionnel pour les notifications par e-mail.
     */
    private static String generateHtmlTemplate(String subject, String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap');" +
                "  body { font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #f5576c 75%, #4facfe 100%); color: #1a202c; margin: 0; padding: 20px; min-height: 100vh; animation: gradientShift 10s ease infinite; background-size: 400% 400%; }" +
                "  @keyframes gradientShift { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }" +
                "  .wrapper { width: 100%; max-width: 700px; margin: 0 auto; }" +
                "  .content { background: linear-gradient(145deg, #ffffff 0%, #f8fafc 50%, #fef3c7 100%); border-radius: 25px; overflow: hidden; box-shadow: 0 25px 50px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.3), 0 0 50px rgba(99,102,241,0.2); margin: 20px 0; position: relative; }" +
                "  .content::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 5px; background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899, #f59e0b, #10b981, #3b82f6); background-size: 200% 100%; animation: rainbow 3s linear infinite; }" +
                "  @keyframes rainbow { 0% { background-position: 0% 0%; } 100% { background-position: 200% 0%; } }" +
                "  .header { background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 25%, #ec4899 50%, #f59e0b 75%, #10b981 100%); padding: 50px 30px; text-align: center; position: relative; overflow: hidden; }" +
                "  .header::before { content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%); animation: pulse 2s ease-in-out infinite; }" +
                "  .header::after { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.1) 50%, transparent 70%); animation: shimmer 3s infinite; }" +
                "  @keyframes shimmer { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }" +
                "  .header h1 { color: #ffffff; margin: 0; font-size: 36px; font-weight: 900; letter-spacing: 3px; text-transform: uppercase; position: relative; z-index: 1; text-shadow: 0 4px 8px rgba(0,0,0,0.4), 0 0 30px rgba(255,255,255,0.5); }" +
                "  .header .subtitle { color: #fbbf24; margin: 20px 0 0 0; font-size: 24px; font-weight: 800; position: relative; z-index: 1; text-shadow: 0 3px 6px rgba(0,0,0,0.5), 0 0 20px rgba(251,191,36,0.8); background: linear-gradient(135deg, #1e40af, #2563eb, #3b82f6); padding: 15px 25px; border-radius: 15px; display: inline-block; border: 3px solid #fbbf24; box-shadow: 0 8px 25px rgba(30,64,175,0.6), 0 0 30px rgba(251,191,36,0.4); animation: subtitleGlow 2s ease-in-out infinite; }" +
                "  @keyframes subtitleGlow { 0%, 100% { box-shadow: 0 8px 25px rgba(30,64,175,0.6), 0 0 30px rgba(251,191,36,0.4); transform: scale(1); } 50% { box-shadow: 0 12px 35px rgba(30,64,175,0.8), 0 0 50px rgba(251,191,36,0.6); transform: scale(1.05); } }" +
                "  .header .healing-badge { display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-top: 15px; box-shadow: 0 4px 15px rgba(16,185,129,0.4); position: relative; z-index: 1; }" +
                "  .body { padding: 50px 40px; line-height: 1.8; background: linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(248,250,252,0.9) 100%); position: relative; }" +
                "  .body::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: radial-gradient(circle at 20% 80%, rgba(139,92,246,0.1) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(236,72,153,0.1) 0%, transparent 50%); pointer-events: none; }" +
                "  .body h2 { color: #1e293b; margin-top: 0; font-size: 28px; font-weight: 700; margin-bottom: 25px; border-left: 5px solid #3b82f6; padding-left: 20px; background: linear-gradient(135deg, #3b82f6, #8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; position: relative; z-index: 1; }" +
                "  .body p { color: #475569; font-size: 16px; margin-bottom: 20px; position: relative; z-index: 1; }" +
                "  .highlight-box { background: linear-gradient(145deg, #f0f9ff 0%, #e0f2fe 50%, #fef3c7 100%); border-left: 5px solid #0ea5e9; padding: 25px; border-radius: 15px; margin: 30px 0; border: 2px solid #bae6fd; box-shadow: 0 10px 25px rgba(14,165,233,0.2); position: relative; overflow: hidden; }" +
                "  .highlight-box::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, #0ea5e9, #3b82f6, #8b5cf6, #ec4899); }" +
                "  .highlight-box h3 { color: #0c4a6e; margin: 0 0 15px 0; font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 10px; }" +
                "  .highlight-box p { color: #075985; margin: 0; font-size: 16px; font-weight: 500; }" +
                "  .colorful-section { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 25%, #fbbf24 50%, #f59e0b 75%, #d97706 100%); padding: 30px; border-radius: 20px; margin: 30px 0; text-align: center; box-shadow: 0 15px 35px rgba(251,191,36,0.3); position: relative; overflow: hidden; }" +
                "  .colorful-section::before { content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, transparent 70%); animation: rotate 8s linear infinite; }" +
                "  @keyframes rotate { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }" +
                "  .colorful-section h3 { color: #78350f; margin: 0 0 15px 0; font-size: 22px; font-weight: 800; position: relative; z-index: 1; }" +
                "  .colorful-section p { color: #92400e; margin: 0; font-size: 16px; font-weight: 600; position: relative; z-index: 1; }" +
                "  .button { display: inline-block; padding: 18px 36px; background: linear-gradient(135deg, #10b981 0%, #059669 25%, #3b82f6 50%, #8b5cf6 75%, #ec4899 100%); color: #ffffff; text-decoration: none; border-radius: 15px; font-weight: 700; font-size: 16px; margin: 30px 0; transition: all 0.4s ease; box-shadow: 0 8px 25px rgba(16,185,129,0.4); text-transform: uppercase; letter-spacing: 2px; position: relative; overflow: hidden; background-size: 300% 300%; animation: buttonGradient 4s ease infinite; }" +
                "  @keyframes buttonGradient { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }" +
                "  .button::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); transition: left 0.5s; }" +
                "  .button:hover::before { left: 100%; }" +
                "  .button:hover { transform: translateY(-3px) scale(1.05); box-shadow: 0 15px 40px rgba(16,185,129,0.6); }" +
                "  .status-tag { display: inline-block; padding: 10px 20px; border-radius: 30px; background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%); color: #78350f; font-weight: 700; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; box-shadow: 0 5px 15px rgba(251,191,36,0.4); position: relative; overflow: hidden; }" +
                "  .status-tag::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent); animation: shimmer 2s infinite; }" +
                "  .footer { background: linear-gradient(145deg, #1e293b 0%, #0f172a 50%, #581c87 100%); padding: 40px; text-align: center; border-top: 5px solid; border-image: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899, #f59e0b, #10b981) 1; position: relative; overflow: hidden; }" +
                "  .footer::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: radial-gradient(circle at 50% 50%, rgba(139,92,246,0.1) 0%, transparent 70%); }" +
                "  .footer .social-links { margin: 20px 0; position: relative; z-index: 1; }" +
                "  .footer .social-links a { display: inline-block; margin: 0 12px; width: 45px; height: 45px; background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0.2)); border-radius: 50%; line-height: 45px; text-align: center; color: #e2e8f0; text-decoration: none; transition: all 0.3s ease; font-size: 20px; border: 2px solid rgba(255,255,255,0.2); }" +
                "  .footer .social-links a:hover { background: linear-gradient(135deg, #3b82f6, #8b5cf6); color: #ffffff; transform: translateY(-3px) rotate(360deg); box-shadow: 0 10px 25px rgba(59,130,246,0.5); }" +
                "  .footer p { color: #e2e8f0; font-size: 14px; margin: 10px 0; position: relative; z-index: 1; }" +
                "  .footer .copyright { color: #94a3b8; font-size: 12px; margin-top: 25px; padding-top: 25px; border-top: 1px solid #334155; position: relative; z-index: 1; }" +
                "  @keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.5; } 50% { transform: scale(1.1); opacity: 0.8; } }" +
                "  .healing-icons { display: flex; justify-content: center; gap: 20px; margin: 20px 0; position: relative; z-index: 1; }" +
                "  .healing-icon { width: 50px; height: 50px; background: linear-gradient(135deg, #10b981, #3b82f6); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; color: white; box-shadow: 0 5px 15px rgba(16,185,129,0.4); animation: bounce 2s infinite; }" +
                "  .healing-icon:nth-child(2) { animation-delay: 0.5s; background: linear-gradient(135deg, #8b5cf6, #ec4899); }" +
                "  .healing-icon:nth-child(3) { animation-delay: 1s; background: linear-gradient(135deg, #f59e0b, #ef4444); }" +
                "  @keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }" +
                "  @media (max-width: 600px) { .content { margin: 10px; border-radius: 20px; } .header { padding: 40px 20px; } .header h1 { font-size: 28px; } .body { padding: 35px 25px; } .healing-icons { flex-wrap: wrap; } }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='wrapper'>" +
                "  <div class='content'>" +
                "    <div class='header'>" +
                "      <h1>SKYLORA SUPPORT</h1>" +
                "      <div class='subtitle'>✨ Votre Solution de Support & Healing Premium ✨</div>" +
                "      <div class='healing-badge'>🌟 HEALING CENTER 🌟</div>" +
                "    </div>" +
                "    <div class='body'>" +
                "      <h2>" + subject + "</h2>" +
                "      <p>" + content.replace("\n", "<br>") + "</p>" +
                "      <div class='healing-icons'>" +
                "        <div class='healing-icon'>💚</div>" +
                "        <div class='healing-icon'>💙</div>" +
                "        <div class='healing-icon'>💛</div>" +
                "      </div>" +
                "      <div class='highlight-box'>" +
                "        <h3>🚀 Prochaines Étapes</h3>" +
                "        <p>Nous traitons votre demande avec la plus haute priorité. Notre équipe technique vous contactera dans les plus brefs délais.</p>" +
                "      </div>" +
                "      <div class='colorful-section'>" +
                "        <h3>🌈 Votre Bien-être est Notre Priorité</h3>" +
                "        <p>Skylora combine technologie et bien-être pour vous offrir le meilleur service de support.</p>" +
                "      </div>" +
                "      <div class='status-tag'>🎟️ Ticket En Cours de Healing</div>" +
                "      <a href='https://skylora.tn' class='button'>🎯 Accéder à mon Compte</a>" +
                "      <p style='margin-top: 40px; font-size: 18px; color: #1e293b; font-weight: 600; position: relative; z-index: 1;'><strong>Merci de faire confiance à Skylora</strong> pour votre support technique. Votre satisfaction est notre priorité absolue !</p>" +
                "      <p style='color: #64748b; font-style: italic; font-size: 16px; position: relative; z-index: 1;'>Nous vous remercions de votre patience et de votre compréhension.</p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <div class='social-links'>" +
                "        <a href='#' title='Facebook'>📘</a>" +
                "        <a href='#' title='Twitter'>🐦</a>" +
                "        <a href='#' title='LinkedIn'>💼</a>" +
                "        <a href='#' title='Instagram'>📷</a>" +
                "        <a href='#' title='YouTube'>📺</a>" +
                "      </div>" +
                "      <p><strong>📧 Nous Contacter</strong></p>" +
                "      <p>support@skylora.tn | +216 70 000 000</p>" +
                "      <p><strong>🏢 Siège Social</strong></p>" +
                "      <p>Tunis, Tunisia - Technopark El Ghazala</p>" +
                "      <p><strong>🌟 Healing Center</strong></p>" +
                "      <p>Ouvert 24/7 pour votre bien-être</p>" +
                "      <div class='copyright'>" +
                "        &copy; 2024 Skylora Tunisia. Tous droits réservés.<br>" +
                "        Ceci est un message automatique, merci de ne pas y répondre.<br>" +
                "        Conçu avec ❤️ et 🌈 en Tunisie 🇹🇳<br>" +
                "        Powered by Healing Technology ✨" +
                "      </div>" +
                "    </div>" +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

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
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "Skylora Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject(subject);

            // Create HTML content
            String htmlContent = generateHtmlTemplate(subject, content);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email HTML envoyé avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
