package com.skilora.finance.service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Chatbot pour la partie Finance (Ma Paie).
 * Si une clé OpenAI est configurée (openai.api.key dans config.properties), les réponses
 * sont générées par l'IA (GPT). Sinon, réponses par règles (FAQ paie).
 * Répond uniquement aux questions liées à la paie, bulletins, CNSS, IRPP, contrats, primes, comptes bancaires.
 * Refuse poliment toute question hors sujet.
 */
public class FinanceChatbotService {

    private static final FinanceChatbotService INSTANCE = new FinanceChatbotService();
    private final FinanceChatbotAIService aiService = new FinanceChatbotAIService();

    /** Mots-clés qui indiquent une question liée à la finance / paie (au moins un doit matcher). */
    private static final String[] FINANCE_KEYWORDS = {
            "salaire", "paie", "paye", "bulletin", "net", "brut", "cnss", "irpp",
            "prime", "primes", "contrat", "compte", "bancaire", "iban", "banque",
            "deduction", "déduction", "retenue", "retenues", "impot", "impôt", "taxe",
            "remuneration", "rémunération", "heures sup", "heures supp", "overtime",
            "tnd", "devise", "currency", "versement", "virement", "paiement",
            "cotisation", "tranche", "barème", "fiscal", "fiscale",
            "patronal", "patronale", "salarial", "salariale", "masse salariale",
            "fiche de paie", "bulletins", "relevé", "solde", "net à payer",
            "salaire de base", "base salary", "brut mensuel", "net mensuel",
            "assurance", "maladie", "retraite", "caisse", "social"
    };

    /** Pattern pour détecter un sujet hors finance (réponses génériques). */
    private static final Pattern OFF_TOPIC_PATTERNS = Pattern.compile(
            "\\b(météo|weather|football|sport|recette|cuisine|film|cinéma|musique|politique|actualité|news|blague|joke|hello|bonjour|salut|hi)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static FinanceChatbotService getInstance() {
        return INSTANCE;
    }

    private FinanceChatbotService() {}

    /**
     * Vérifie si la question est liée au domaine finance/paie.
     */
    public boolean isFinanceRelated(String question) {
        if (question == null || question.isBlank())
            return false;
        String q = normalize(question);
        if (q.length() < 2)
            return false;
        // Refus explicite des sujets hors travail
        if (OFF_TOPIC_PATTERNS.matcher(q).find())
            return false;
        for (String kw : FINANCE_KEYWORDS) {
            if (q.contains(kw))
                return true;
        }
        // Questions très courtes type "c'est quoi le net ?" ou "explique moi la cnss"
        if (q.matches(".*\\b(net|brut|cnss|irpp|prime|paie|salaire|bulletin)\\b.*"))
            return true;
        return false;
    }

    /**
     * Indique si le chatbot utilise l'IA (OpenAI) pour répondre.
     */
    public boolean isUsingAI() {
        return aiService.isConfigured();
    }

    /**
     * Répond à l'utilisateur. Si une clé OpenAI est configurée, l'IA génère la réponse ;
     * sinon (ou en cas d'erreur API), réponses par règles (FAQ paie).
     *
     * @param question question de l'utilisateur
     * @param context  contexte optionnel (nom employé, dernier net, période, etc.) pour personnaliser
     */
    public String answer(String question, FinanceChatContext context) {
        if (question == null || question.isBlank())
            return "Posez-moi une question sur votre paie, vos bulletins, les déductions (CNSS, IRPP), les primes ou les comptes bancaires.";

        // Si l'IA est configurée, on tente d'abord une réponse par l'IA (OpenAI)
        if (aiService.isConfigured()) {
            String aiResponse = aiService.askAI(question, context);
            if (aiResponse != null && !aiResponse.isBlank())
                return aiResponse;
            // En cas d'erreur API (réseau, quota, etc.), repli sur les règles
        }

        String q = normalize(question);

        if (!isFinanceRelated(question)) {
            return "Je ne réponds qu’aux questions sur la gestion finance et la paie (bulletins, salaire, CNSS, IRPP, primes, contrats, comptes bancaires). Pour toute autre sujet, merci de vous adresser aux ressources concernées.";
        }

        // Réponses par intention (règles) — utilisées si IA désactivée ou en repli
        if (matches(q, "salut", "bonjour", "hello", "coucou", "bonsoir"))
            return "Bonjour ! Je suis l’assistant Ma Paie. Posez-moi une question sur votre rémunération, vos bulletins ou les déductions.";

        if (matches(q, "c quoi le net", "c'est quoi le net", "qu'est ce que le net", "net a payer", "net à payer"))
            return "Le **salaire net à payer** est ce qui vous est effectivement versé après déduction de la part salariale CNSS (9,18 %) et de l’IRPP (impôt sur le revenu selon le barème). C’est le montant qui apparaît sur votre bulletin en bas de page.";

        if (matches(q, "c quoi le brut", "salaire brut", "c'est quoi le brut"))
            return "Le **salaire brut** est le total avant déductions : salaire de base + heures supplémentaires + primes. Les cotisations CNSS et l’IRPP sont calculées sur ce montant.";

        if (matches(q, "cnss", "cotisation cnss", "part cnss"))
            return "La **CNSS** (Caisse Nationale de Sécurité Sociale) est une cotisation sociale. La part salariale est de 9,18 % sur le salaire brut (retenue sur votre paie). L’employeur verse en plus une part patronale (16,57 %).";

        if (matches(q, "irpp", "impot", "impôt", "impot sur le revenu"))
            return "L’**IRPP** (Impôt sur le Revenu des Personnes Physiques) est l’impôt sur le revenu en Tunisie. Il est calculé selon un barème progressif (tranches) et retenu à la source sur votre bulletin.";

        if (matches(q, "prime", "primes", "bonus"))
            return "Les **primes** sont des montants ajoutés à votre rémunération (performance, ancienneté, etc.). Elles apparaissent sur le bulletin et sont prises en compte dans le brut pour le calcul des cotisations.";

        if (matches(q, "bulletin", "fiche de paie", "bulletins"))
            return "Le **bulletin de paie** récapitule pour une période donnée : salaire de base, heures sup., primes, déductions (CNSS, IRPP) et le net à payer. Vous pouvez le consulter dans l’onglet « Historique des Paies ».";

        if (matches(q, "contrat", "type de contrat", "cdi", "cdd"))
            return "Votre **contrat** définit votre poste, le type (CDI, CDD…), le salaire et les dates. Les détails sont visibles dans la section « Détails du Contrat » de ce tableau de bord.";

        if (matches(q, "compte bancaire", "iban", "virement", "versement"))
            return "Les **comptes bancaires** enregistrés sont utilisés pour le virement de votre salaire. L’IBAN apparaît masqué pour la sécurité. Vous pouvez consulter la liste dans l’onglet « Comptes Bancaires ».";

        if (matches(q, "heures sup", "heures supp", "overtime"))
            return "Les **heures supplémentaires** sont rémunérées en plus du salaire de base. Leur montant et leur nombre figurent sur chaque bulletin de paie.";

        if (matches(q, "deduction", "déduction", "retenue"))
            return "Les **déductions** sur le bulletin sont principalement : la part salariale CNSS (9,18 %) et l’IRPP (selon le barème). Le net à payer = brut − déductions.";

        if (matches(q, "mon net", "mon salaire", "combien je gagne", "dernier bulletin"))
            return buildPersonalizedNetAnswer(context);

        // Question sur le salaire d'un employé précis (ex: "salaire de John Doe", "quel est le salaire de Marie")
        String employeeSalaryAnswer = buildEmployeeSalaryAnswer(question, context);
        if (employeeSalaryAnswer != null)
            return employeeSalaryAnswer;

        if (matches(q, "aide", "help", "aide moi", "comment"))
            return "Je peux vous expliquer : le salaire net et brut, la CNSS, l’IRPP, les primes, les bulletins de paie, les contrats et les comptes bancaires. Posez une question précise sur l’un de ces sujets.";

        // Réponse par défaut pour questions finance reconnues mais sans règle spécifique
        return "Votre question concerne bien la paie ou la finance. Pour une réponse plus précise, reformulez en mentionnant par exemple : bulletin, net, brut, CNSS, IRPP, prime ou compte bancaire.";
    }

    private boolean matches(String normalizedQuestion, String... keywords) {
        for (String kw : keywords) {
            if (normalizedQuestion.contains(kw))
                return true;
        }
        return false;
    }

    private String buildPersonalizedNetAnswer(FinanceChatContext context) {
        if (context != null && context.getLastPayslipNet() != null && context.getLastPayslipPeriod() != null) {
            return String.format(Locale.FRANCE,
                    "D’après vos données : **dernier bulletin** (%s), le **salaire net à payer** est de **%s TND**. Ce montant est celui qui vous est versé après déductions.",
                    context.getLastPayslipPeriod(),
                    context.getLastPayslipNet());
        }
        return "Le **salaire net** est indiqué sur chaque bulletin de paie (en bas). Vous pouvez consulter votre « Historique des Paies » dans ce tableau de bord pour voir le net de chaque période.";
    }

    /**
     * Si la question mentionne un nom d'employé (ex: "salaire de John Doe"), cherche dans le contexte
     * et retourne une réponse personnalisée avec le salaire de cet employé, sinon null.
     */
    private String buildEmployeeSalaryAnswer(String question, FinanceChatContext context) {
        if (context == null || context.getEmployeesByNormalizedName() == null || context.getEmployeesByNormalizedName().isEmpty())
            return null;
        String q = question.trim();
        for (Map.Entry<String, EmployeeSnapshot> e : context.getEmployeesByNormalizedName().entrySet()) {
            String fullName = e.getValue().getFullName();
            String normalizedName = normalize(fullName);
            if (normalizedName.isEmpty()) continue;
            String normalizedQ = normalize(q);
            if (normalizedQ.contains(normalizedName)
                    || (fullName != null && fullName.length() > 0 && normalizedQ.contains(normalize(fullName.split("\\s+")[0])))) {
                EmployeeSnapshot snap = e.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append("D'après les données : **").append(fullName).append("**.");
                if (snap.getLastPayslipNet() != null && !snap.getLastPayslipNet().isEmpty()) {
                    sb.append(" Salaire net (dernier bulletin");
                    if (snap.getLastPayslipPeriod() != null && !snap.getLastPayslipPeriod().isEmpty())
                        sb.append(", ").append(snap.getLastPayslipPeriod());
                    sb.append(") : **").append(snap.getLastPayslipNet()).append(" TND**.");
                }
                if (snap.getCurrentSalary() != null && !snap.getCurrentSalary().isEmpty())
                    sb.append(" Salaire de base contractuel : **").append(snap.getCurrentSalary()).append(" TND**.");
                if (sb.length() <= fullName.length() + 30)
                    sb.append(" Aucun bulletin ou contrat enregistré pour le moment.");
                return sb.toString();
            }
        }
        return null;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.FRENCH)
                .replaceAll("[àâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Contexte optionnel pour personnaliser les réponses (dernier bulletin, nom, etc.)
     * et pour répondre aux questions sur le salaire d'autres employés.
     */
    public static class FinanceChatContext {
        private final String employeeName;
        private final String lastPayslipPeriod;
        private final String lastPayslipNet;
        private final Map<String, EmployeeSnapshot> employeesByNormalizedName;

        public FinanceChatContext(String employeeName, String lastPayslipPeriod, String lastPayslipNet) {
            this(employeeName, lastPayslipPeriod, lastPayslipNet, null);
        }

        public FinanceChatContext(String employeeName, String lastPayslipPeriod, String lastPayslipNet,
                                  Map<String, EmployeeSnapshot> employeesByNormalizedName) {
            this.employeeName = employeeName;
            this.lastPayslipPeriod = lastPayslipPeriod;
            this.lastPayslipNet = lastPayslipNet;
            this.employeesByNormalizedName = employeesByNormalizedName != null ? employeesByNormalizedName : Map.of();
        }

        public String getEmployeeName() { return employeeName; }
        public String getLastPayslipPeriod() { return lastPayslipPeriod; }
        public String getLastPayslipNet() { return lastPayslipNet; }
        public Map<String, EmployeeSnapshot> getEmployeesByNormalizedName() { return employeesByNormalizedName; }
    }

    /** Données d'un employé pour les réponses personnalisées (salaire de X). */
    public static class EmployeeSnapshot {
        private final String fullName;
        private final String lastPayslipNet;
        private final String lastPayslipPeriod;
        private final String currentSalary;

        public EmployeeSnapshot(String fullName, String lastPayslipNet, String lastPayslipPeriod, String currentSalary) {
            this.fullName = fullName;
            this.lastPayslipNet = lastPayslipNet;
            this.lastPayslipPeriod = lastPayslipPeriod;
            this.currentSalary = currentSalary;
        }

        public String getFullName() { return fullName; }
        public String getLastPayslipNet() { return lastPayslipNet; }
        public String getLastPayslipPeriod() { return lastPayslipPeriod; }
        public String getCurrentSalary() { return currentSalary; }
    }
}
