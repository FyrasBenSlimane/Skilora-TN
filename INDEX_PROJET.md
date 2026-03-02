# 📌 INDEX PROJET — Tout trouver vite (validation / professeur)

**À ouvrir en premier** quand le professeur pose une question : ce fichier te dit **où est quoi**.

---

## 1. Répondre aux questions du professeur

| Le prof demande… | Où regarder |
|------------------|-------------|
| **Tout sur les APIs et métiers avancés Finance** | **`docs/GUIDE_VALIDATION_FINANCE_API_ET_METIERS.md`** ← document principal (ou **`GUIDE_VALIDATION_FINANCE.md`** à la racine pour le lien rapide) |
| **Où sont les clés API ?** | **`src/main/resources/config.properties`** |
| **Structure du module Finance** | **`docs/FINANCE_ORGANISATION_ET_METIERS_AVANCES.md`** |

---

## 2. Structure du projet (simplifiée)

```
JAVAFX/
├── INDEX_PROJET.md              ← TU ES ICI (point d'entrée — où est quoi)
├── GUIDE_VALIDATION_FINANCE.md  ← Lien rapide vers le guide complet
├── README.md                    ← Présentation du projet
├── README_FINANCE.md            ← Démarrage rapide Finance
│
├── docs/                        ← DOCUMENTATION
│   ├── README.md                ← Liste des docs
│   ├── GUIDE_VALIDATION_FINANCE_API_ET_METIERS.md   ← Réponses au prof (APIs, IA, métiers)
│   └── FINANCE_ORGANISATION_ET_METIERS_AVANCES.md   ← Organisation + emplacements
│
├── src/main/
│   ├── java/com/skilora/
│   │   ├── Main.java        ← Point d'entrée application
│   │   ├── ui/MainView.java ← Menu principal, chargement des vues
│   │   │
│   │   └── finance/         ← TOUT LE MODULE FINANCE
│   │       ├── controller/   (écrans)
│   │       ├── service/       (métier + APIs)
│   │       ├── model/         (données)
│   │       ├── utils/         (PDF, validation…)
│   │       └── util/          (DB)
│   │
│   └── resources/
│       ├── config.properties        ← Clés API (Stripe, Twilio, OpenAI)
│       ├── finance/views/           ← Vues FXML Finance (Ma Paie, Admin, Paiement…)
│       └── com/skilora/finance/     ← Template PDF
│
└── (autres dossiers : recruitment, framework, etc.)
```

---

## 3. Gestion Finance — Où c’est

| Élément | Chemin |
|--------|--------|
| **Code Finance** | `src/main/java/com/skilora/finance/` |
| **Vues FXML** | `src/main/resources/finance/views/` (FinanceView, EmployeurFinanceView, UserFinanceView, paiement) |
| **Config (clés API)** | `src/main/resources/config.properties` |
| **Template rapport PDF** | `src/main/resources/com/skilora/finance/rapport_financier_template.html` |

---

## 4. APIs — Où c’est

| API | Fichier principal | Clé dans config |
|-----|-------------------|-----------------|
| **Stripe** (paiements) | `finance/service/StripePaymentService.java` | `stripe.secret.key`, `stripe.public.key` |
| **Twilio** (WhatsApp) | `finance/service/SmsService.java` | `twilio.account.sid`, `twilio.auth.token`, etc. |
| **OpenAI** (IA) | `finance/service/FinanceChatbotAIService.java` | `openai.api.key` |

---

## 5. Métiers avancés / IA — Où c’est

| Fonctionnalité | Fichiers à citer |
|----------------|------------------|
| **Chatbot Ma Paie (IA)** | `FinanceChatbotService.java`, `FinanceChatbotAIService.java`, `EmployeurFinanceController.java`, vue `EmployeurFinanceView.fxml` |
| **Résumé PDF (IA)** | `FinanceController.buildProfessionalSummaryWithAI`, `buildRawDataForAISummary`, `FinanceChatbotAIService.generateReportSummary`, repli `buildProfessionalSummary` |
| **Paiement Stripe + BDD** | `PaiementController.java`, `StripePaymentService.java`, `PaiementService.java`, `paiement.fxml` |
| **Calculs fiscaux** | `TaxCalculationService.java` |
| **Génération PDF** | `PDFGenerator.java`, `rapport_financier_template.html` |

---

## 6. En cas de panique

1. Ouvre **`docs/GUIDE_VALIDATION_FINANCE_API_ET_METIERS.md`** : toutes les réponses courtes et les explications y sont.
2. Ouvre **`config.properties`** pour montrer où sont les clés API.
3. Montre **`FinanceChatbotAIService.java`** pour l’intégration IA (chatbot + résumé PDF).
4. Montre **`FinanceController.java`** (méthodes `buildProfessionalSummaryWithAI`, `buildRawDataForAISummary`) pour le résumé PDF avec IA.

---

*Dernière mise à jour : structure et index pour validation.*
