# Gestion Finance Skilora — Organisation des dossiers et métiers avancés

Ce document décrit l’organisation des dossiers liés à la **Gestion Finance** et l’emplacement de chaque **métier avancé**, **API** et **fonctionnalité intégrée** (dont celles à caractère « intelligent »).

---

## 1. Arborescence des dossiers Finance

### 1.1 Code Java (`src/main/java/com/skilora/finance/`)

```
com.skilora.finance/
├── FinanceApp.java              # Point d’entrée standalone du module Finance
├── Launcher.java                # Lanceur alternatif
│
├── controller/                  # Contrôleurs des vues
│   ├── FinanceController.java       # Admin : contrats, bulletins, primes, banques, génération PDF
│   ├── EmployeurFinanceController.java  # Employé : Ma Paie + chatbot
│   ├── UserFinanceController.java      # Utilisateur : consultation finance
│   ├── PaiementController.java        # Paiement avance projet (Stripe)
│   └── EmployeeDashboardController.java
│
├── service/                     # Métier et API
│   ├── FinanceService.java          # Données (contrats, bulletins, primes, comptes)
│   ├── FinanceChatbotService.java   # Chatbot Finance (réponses règles, personnalisées)
│   ├── StripePaymentService.java    # API Stripe (paiements)
│   ├── PaiementService.java         # CRUD paiements en base
│   ├── TaxCalculationService.java   # Calculs fiscaux (IRPP, CNSS, etc.)
│   └── SmsService.java              # Envoi SMS (optionnel)
│
├── model/                       # Modèles métier / DTO
│   ├── ContractRow.java, EmploymentContract.java
│   ├── PayslipRow.java, Payslip.java
│   ├── BonusRow.java, Bonus.java
│   ├── BankAccountRow.java, BankAccount.java
│   ├── EmployeeSummaryRow.java
│   ├── Paiement.java
│   └── ExchangeRate.java
│
├── utils/                       # Utilitaires
│   ├── PDFGenerator.java            # Génération PDF/HTML des rapports financiers
│   ├── CurrencyHelper.java
│   └── ValidationHelper.java
│
├── util/                        # Connexion et initialisation
│   ├── DatabaseConnection.java
│   └── DatabaseInitializer.java
```

### 1.2 Ressources (`src/main/resources/`)

- **Vues Finance (FXML)** — tout au même endroit pour plus de visibilité :
  - `finance/views/FinanceView.fxml`         → Vue Admin (onglets contrats, bulletins, primes, banques, PDF)
  - `finance/views/EmployeurFinanceView.fxml` → Vue « Ma Paie » (employé) + zone chatbot
  - `finance/views/UserFinanceView.fxml`      → Vue consultation utilisateur
  - `finance/views/paiement.fxml`              → Vue Paiement avance projet (Stripe)

- **Template rapport PDF** :
  - `com/skilora/finance/rapport_financier_template.html` → Modèle HTML utilisé par `PDFGenerator`

- **Ancien emplacement (conservé, non supprimé)** :
  - `fxml/FinanceView.fxml`, `fxml/EmployeurFinanceView.fxml`, `fxml/UserFinanceView.fxml`  
  → L’application charge désormais les vues depuis `finance/views/` (voir `MainView.java`).

---

## 2. Métiers avancés et API — Où se trouve quoi

### 2.1 Chatbot Finance — avec IA (OpenAI) ou par règles

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Service** | `com.skilora.finance.service.FinanceChatbotService` | Singleton. Détecte si la question est liée à la finance, refuse poliment le hors-sujet, répond par règles (FAQ paie). Gère les questions personnalisées (« salaire de X ») via le contexte. |
| **Contexte** | `FinanceChatbotService.FinanceChatContext` et `EmployeeSnapshot` | Données injectées (nom employé, dernier bulletin, liste des employés avec net/période/salaire) pour réponses personnalisées. |
| **Utilisation** | `EmployeurFinanceController` | `buildChatContext()` construit le contexte ; `handleChatSend()` appelle `chatbotService.answer(question, context)` et affiche la réponse dans la zone chat. |
| **Vue** | `finance/views/EmployeurFinanceView.fxml` | Zone de chat (champ question + bouton envoyer, liste des messages). |

**Résumé** : Pas d’API externe ni de LLM. Comportement « intelligent » par mots-clés, patterns et règles métier ; personnalisation via les données employés passées dans le contexte.

---

### 2.2 Résumé automatique du rapport PDF (résumé professionnel par employé)

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Construction du résumé** | `FinanceController.buildProfessionalSummary(int empId, String employeeName)` | Construit un paragraphe unique par employé : poste, type de contrat, salaire, nombre de bulletins, dernier net, primes, comptes bancaires. Données issues des listes déjà chargées (contrats, bulletins, primes, banques). |
| **Injection dans le PDF** | `FinanceController.handleGenerateEmployeeReport()` | Appelle `buildProfessionalSummary(empId, empName)` puis passe le texte en `customSummary` à `PDFGenerator.generateEmployeeReport(...)`. |
| **Génération PDF** | `com.skilora.finance.utils.PDFGenerator` | `generateEmployeeReport(..., customSummary)` : si `customSummary` non vide, il est utilisé pour la section « Résumé » du rapport ; sinon ancien résumé générique. Utilise le template `rapport_financier_template.html`. |
| **Template** | `com/skilora/finance/rapport_financier_template.html` | HTML/CSS du rapport (titres, deux-points, style professionnel). |

**Résumé** : Pas d’API externe ni de LLM. « Résumé automatique » = logique métier dans `buildProfessionalSummary` + intégration dans le flux PDF existant.

---

### 2.3 Paiement Stripe (API externe)

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Service API** | `com.skilora.finance.service.StripePaymentService` | Appels HTTP à l’API Stripe (création PaymentMethod, PaymentIntent). Mode TEST : mapping cartes test (ex. 4242…) vers IDs Stripe prédéfinis (`pm_card_visa`, etc.). Clé secrète lue depuis `config.properties`. |
| **Contrôleur** | `com.skilora.finance.controller.PaiementController` | Utilise `StripePaymentService` pour créer l’intention de paiement et enregistre `stripe_payment_id` dans la base via `PaiementService`. |
| **Modèle / persistance** | `Paiement.java`, `PaiementService.java` | Entité `Paiement` avec `stripePaymentId` ; `PaiementService` pour CRUD en base. |
| **Vue** | `finance/views/paiement.fxml` | Formulaire (bénéficiaire, montant, carte test, etc.). |
| **Point d’entrée** | `MainView.showPaiementView()` | Charge `/finance/views/paiement.fxml`. |

**Résumé** : Métier avancé = intégration API Stripe (paiements) + persistance des paiements en base.

---

### 2.4 Calculs fiscaux (IRPP, CNSS, etc.)

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Service** | `com.skilora.finance.service.TaxCalculationService` | Barèmes, calcul des cotisations et impôts. |
| **Utilisation** | Utilisé par la logique bulletins / rapports (FinanceService, FinanceController, etc.) selon les besoins. | |

---

### 2.5 Génération PDF des rapports financiers

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Générateur** | `com.skilora.finance.utils.PDFGenerator` | Génère le rapport complet (HTML puis PDF via OpenHTML-to-PDF, avec repli iText/OpenPDF). Gère le résumé personnalisé (`customSummary`), les sections contrats, bulletins, primes, banques. |
| **Template** | `com/skilora/finance/rapport_financier_template.html` | Structure et style du rapport. |
| **Données** | `FinanceController` | Construit les blocs HTML (contrats, bulletins, primes, banques) et le résumé professionnel, puis appelle `PDFGenerator.generateEmployeeReport(...)`. |

---

### 2.6 SMS (optionnel)

| Élément | Emplacement | Rôle |
|--------|-------------|------|
| **Service** | `com.skilora.finance.service.SmsService` | Envoi de SMS (intégration possible avec un fournisseur). |

---

## 3. Où sont chargées les vues Finance dans l’application principale

Dans `com.skilora.ui.MainView` :

- **Finance (Admin)** : `showFinanceView()` → charge `/finance/views/FinanceView.fxml`
- **Ma Paie (Employé)** : `showEmployeurFinanceView()` → charge `/finance/views/EmployeurFinanceView.fxml`
- **Finance (User)** : `showUserFinanceView()` → charge `/finance/views/UserFinanceView.fxml`
- **Paiement** : `showPaiementView()` → charge `/finance/views/paiement.fxml`

Toutes les vues Finance sont donc regroupées sous `resources/finance/views/` et référencées par ces chemins.

---

## 4. Récapitulatif des fichiers « métiers avancés / API / IA »

| Fonctionnalité | Fichiers principaux |
|----------------|---------------------|
| **Chatbot Finance** | `FinanceChatbotService.java`, `EmployeurFinanceController.java`, `EmployeurFinanceView.fxml` |
| **Résumé automatique PDF** | `FinanceController.buildProfessionalSummary` + `PDFGenerator.java` + `rapport_financier_template.html` |
| **API Stripe** | `StripePaymentService.java`, `PaiementController.java`, `finance/views/paiement.fxml` |
| **Calculs fiscaux** | `TaxCalculationService.java` |
| **Génération PDF** | `PDFGenerator.java`, `rapport_financier_template.html`, `FinanceController` (buildContractInfo, buildPayslipInfo, etc.) |
| **Données Finance** | `FinanceService.java`, modèles dans `model/` |

Aucun fichier ni dossier du projet n’a été supprimé : les anciens FXML dans `fxml/` sont conservés ; l’application utilise les copies dans `finance/views/` pour un module Finance plus visible et ordonné.
