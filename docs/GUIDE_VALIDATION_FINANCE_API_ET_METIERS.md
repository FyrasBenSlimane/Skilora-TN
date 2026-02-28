# Guide complet — APIs et métiers avancés de la Gestion Finance (Skilora)

Ce document explique **tout** ce qui a été ajouté à la gestion finance : les **API externes** (Stripe, Twilio, **OpenAI**) et les **métiers avancés** (chatbot, résumé PDF, calculs fiscaux, génération PDF, workflow paiement). Il sert à préparer la validation et à répondre aux questions du professeur.

**Deux métiers sont intégrés avec l’IA (OpenAI) :**
1. **Chatbot Ma Paie** : réponses en langage naturel (avec repli par règles si pas de clé ou erreur).
2. **Résumé du rapport PDF** : paragraphe de résumé rédigé par l’IA à partir des données employé (avec repli par logique métier si pas de clé ou erreur).

---

## PARTIE 1 — LES API EXTERNES

### 1.1 API Stripe (paiements en ligne)

**À quoi ça sert ?**  
Permettre de payer un « avance projet » par carte bancaire en mode **TEST** (aucun vrai argent). Stripe est une plateforme de paiement utilisée partout dans le monde.

**Où c’est dans le projet ?**
- **Service** : `com.skilora.finance.service.StripePaymentService`
- **Configuration** : `src/main/resources/config.properties`  
  - `stripe.secret.key` = clé secrète Stripe (sk_test_...)  
  - `stripe.public.key` = clé publique (pk_test_...)
- **Utilisation** : `PaiementController` appelle ce service quand on clique sur « Payer ».

**Comment ça marche (flux en 3 étapes) ?**

1. **Résolution du moyen de paiement (Payment Method)**  
   - L’utilisateur saisit numéro de carte, date d’expiration, CVV.  
   - En mode TEST, Stripe interdit d’envoyer les vrais numéros depuis le serveur (sécurité).  
   - Donc on utilise des **cartes test** (ex. 4242 4242 4242 4242).  
   - Le service a une **map** : numéro test → ID Stripe prédéfini (`pm_card_visa`, `pm_card_mastercard`, etc.).  
   - Méthode : `resolvePaymentMethodId(cardNumber, expMonth, expYear, cvc)`  
   - Si la carte est une carte test connue → retourne `pm_card_xxx`. Sinon → tentative de création d’un PaymentMethod via l’API Stripe.

2. **Création du PaymentIntent**  
   - On envoie à Stripe : montant (en centimes), devise (USD), type de paiement (carte).  
   - Stripe répond avec un **PaymentIntent** (id du type `pi_XXXX`).  
   - Méthode : `createPaymentIntent(montantCentimes)`.

3. **Confirmation du paiement**  
   - On envoie à Stripe : l’id du PaymentIntent + l’id du Payment Method.  
   - Stripe confirme (ou demande 3D Secure en test).  
   - Méthode : `confirmPayment(paymentIntentId, paymentMethodId)`  
   - Retourne `true` si statut = `succeeded` ou `requires_action` (3DS).

**Technique** : appels HTTP avec `HttpClient` (Java 11+) vers `https://api.stripe.com/v1/...`, en `POST`, avec le header `Authorization: Bearer <secret_key>`.

**Points à retenir pour le professeur**  
- Stripe = API externe pour paiement par carte.  
- Mode TEST = cartes test (4242…), pas d’argent réel.  
- Pas de SDK Stripe dans le projet : tout est fait en HTTP avec `HttpClient`.  
- Les clés sont dans `config.properties` pour ne pas les mettre en dur dans le code.

---

### 1.2 API Twilio (WhatsApp / messages)

**À quoi ça sert ?**  
Envoyer un message **WhatsApp** de confirmation après un paiement réussi (bénéficiaire, montant, projet, date, référence Stripe).

**Où c’est dans le projet ?**
- **Service** : `com.skilora.finance.service.SmsService` (singleton)
- **Configuration** : `config.properties`  
  - `twilio.account.sid` = identifiant du compte Twilio (commence par AC)  
  - `twilio.auth.token` = token d’authentification  
  - `twilio.from.number` = numéro expéditeur (ex. whatsapp:+14155238886)  
  - `twilio.to.number` = numéro destinataire (ex. whatsapp:+216...)

**Comment ça marche ?**
- Après un paiement Stripe réussi, `PaiementController` appelle :  
  `SmsService.getInstance().sendPaymentSuccess(montant, beneficiaire, referenceProjet, intentId)`
- Le service construit un message personnalisé (bénéficiaire, montant, projet, date, ID Stripe).
- Envoi HTTP **POST** vers :  
  `https://api.twilio.com/2010-04-01/Accounts/{accountSid}/Messages.json`  
  avec **Basic Auth** : `AccountSid:AuthToken` encodé en Base64.
- Paramètres : `To`, `From`, `Body` (le texte du message).

**Points à retenir**  
- Twilio = API externe pour envoyer des SMS / messages WhatsApp.  
- Authentification = Basic Auth (Account SID + Auth Token).  
- Si les clés ne sont pas dans `config.properties`, le service affiche « Twilio non configuré » et n’envoie rien (pas de crash).

---

## PARTIE 2 — LES MÉTIERS AVANCÉS

### 2.1 Chatbot Finance (assistant Ma Paie) — avec IA (OpenAI)

**À quoi ça sert ?**  
Un assistant qui répond **uniquement** aux questions sur la paie et la finance (salaire, bulletins, CNSS, IRPP, primes, contrats, comptes bancaires). Il refuse poliment les questions hors sujet (météo, sport, etc.).  
**Avec une clé OpenAI configurée**, les réponses sont générées par l’**IA (GPT)** ; sinon, le chatbot utilise des **règles métier** (repli automatique).

**Où c’est ?**
- **Service principal** : `com.skilora.finance.service.FinanceChatbotService` (singleton)
- **Service IA** : `com.skilora.finance.service.FinanceChatbotAIService` (appel API OpenAI)
- **Contrôleur** : `EmployeurFinanceController` (vue « Ma Paie »)
- **Vue** : `finance/views/EmployeurFinanceView.fxml` (zone de chat)
- **Configuration** : `config.properties` → `openai.api.key` (clé API OpenAI, optionnelle)

**Fonctionnement avec IA (si `openai.api.key` est renseignée)**  
- Chaque question est envoyée à l’API **OpenAI** (modèle `gpt-4o-mini`) avec un **prompt système** qui :  
  - définit l’assistant comme « Ma Paie Skilora », limité aux sujets paie/finance ;  
  - impose un refus poli pour les questions hors sujet ;  
  - injecte le **contexte** (employé connecté, dernier bulletin, liste des employés avec net/salaire) pour des réponses personnalisées (« mon salaire », « salaire de X »).  
- La réponse de l’IA est affichée telle quelle. En cas d’erreur (réseau, quota, clé invalide), le chatbot **replie automatiquement** sur les réponses par règles.

**Fonctionnement sans IA (repli par règles)**  
- **Détection du sujet** : mots-clés (salaire, paie, bulletin, net, brut, cnss, irpp, prime, contrat, compte bancaire, etc.) + pattern hors-sujet (météo, football, etc.).  
- **Réponses** : règles (if/else) pour « c’est quoi le net ? », « CNSS », « mon net », « salaire de X », etc., en s’appuyant sur le même **contexte** (dernier bulletin, liste des employés).

**Contexte**  
- `FinanceChatContext` : nom de l’employé connecté, dernier bulletin (période + net), map (nom normalisé → `EmployeeSnapshot` : nom, dernier net, période, salaire de base).  
- Construit dans `EmployeurFinanceController.buildChatContext()` et passé à la fois à l’IA (dans le prompt) et aux règles.

**Points pour le professeur**  
- Le chatbot peut être **piloté par l’IA** (API OpenAI) : réponses en langage naturel, personnalisées grâce au contexte.  
- **Repli par règles** : si pas de clé OpenAI ou erreur API, le comportement reste déterministe (règles + contexte).  
- **API utilisée** : OpenAI Chat Completions (`https://api.openai.com/v1/chat/completions`), clé dans `config.properties`.

---

### 2.2 Résumé automatique du rapport PDF (par employé)

**À quoi ça sert ?**  
En tête du rapport financier PDF, on affiche un **paragraphe unique** qui résume la situation de l’employé : poste, type de contrat, salaire, nombre de bulletins, dernier net, primes, comptes bancaires. Ce n’est plus un texte générique identique pour tout le monde.

**Où c’est ?**
- **Construction du texte** : `FinanceController.buildProfessionalSummary(int empId, String employeeName)`
- **Utilisation** : `FinanceController.handleGenerateEmployeeReport()` appelle cette méthode puis passe le résultat en `customSummary` à `PDFGenerator.generateEmployeeReport(...)`.
- **PDF** : `PDFGenerator` insère ce texte dans la section « Résumé » du rapport (template HTML).

**Comment c’est fait ?**
- On filtre les listes déjà en mémoire (contrats, bulletins, primes, comptes) pour l’employé choisi.
- Contrat actif → on écrit « X occupe le poste de … en CDI/CDD (salaire de base : … TND). »
- Bulletins → « … N bulletin(s) émis ; le dernier (mois année) s’élève à … TND net. »
- Primes → « … prime(s) versée(s) (total : … TND). »
- Comptes bancaires → « … compte(s) bancaire(s) enregistré(s). »
- Tout est **déterministe**, à partir des données du contrôleur (pas d’API, pas de LLM).

**Points pour le professeur**  
- Résumé « automatique » = logique métier qui agrège les données de l’employé (contrat, bulletins, primes, banques).  
- Chaque employé a un résumé différent car les données sont différentes.  
- Pas d’IA externe.

---

### 2.3 Calculs fiscaux (IRPP, CNSS)

**À quoi ça sert ?**  
Calculer les cotisations et impôts tunisiens : **CNSS** (part salariale 9,18 %, part patronale 16,5 %) et **IRPP** (barème progressif par tranches).

**Où c’est ?**
- **Service** : `com.skilora.finance.service.TaxCalculationService`
- Méthodes statiques : `calculateCompleteSalary(salaryInTND)` retourne une map (brut, CNSS salarial/patronal, revenu imposable, IRPP, déductions totales, net, taux effectif).  
- `calculateProgressiveIRPP(taxableIncome)` applique le barème tunisien (tranches 0 %, 26 %, 28 %, 32 %, 35 %).  
- **Recommandations** : `getOptimizationRecommendations(grossSalary)` donne des conseils selon la tranche (ex. « pas d’IRPP en dessous de 5000 TND », etc.).

**Barème IRPP (simplifié)**  
- 0–5000 TND : 0 %  
- 5000–20000 : 26 %  
- 20000–30000 : 28 %  
- 30000–50000 : 32 %  
- au-delà : 35 %

**Utilisation**  
- Le **calcul** dans l’onglet « Tax » de la vue Admin peut s’appuyer sur ce service (ou une logique similaire en direct dans le contrôleur).  
- Les explications du **chatbot** (CNSS 9,18 %, IRPP progressif) sont cohérentes avec ce service.

**Points pour le professeur**  
- Métier avancé = calcul fiscal tunisien (CNSS + IRPP progressif).  
- Pas d’API externe : formules et barèmes dans le code.  
- « AI-Powered » dans les commentaires = calculs « intelligents » (barème progressif, recommandations), pas un appel à une IA externe.

---

### 2.4 Génération PDF des rapports financiers

**À quoi ça sert ?**  
Produire un **rapport financier** (PDF ou HTML) par employé : résumé personnalisé + contrats + bulletins + primes + comptes bancaires, avec une mise en forme professionnelle (titres, deux-points, majuscules).

**Où c’est ?**
- **Générateur** : `com.skilora.finance.utils.PDFGenerator`
- **Template HTML** : `src/main/resources/com/skilora/finance/rapport_financier_template.html`
- **Données** : `FinanceController` construit les blocs HTML (`buildContractInfo`, `buildBankInfo`, `buildBonusInfo`, `buildPayslipInfo`) et le résumé (`buildProfessionalSummary`), puis appelle `PDFGenerator.generateEmployeeReport(...)`.

**Technique**  
- L’utilisateur choisit l’emplacement du fichier (FileChooser).  
- Génération du HTML complet (template + sections injectées).  
- **PDF** : d’abord tentative avec **OpenHTML-to-PDF** (HTML → PDF). En cas d’erreur (ex. caractères non supportés), **repli** sur **iText/OpenPDF** pour générer le PDF.  
- Chaque information dans le rapport a un **titre en gras** suivi de « : » (ex. « Position dans notre société : Formateur Java »).

**Points pour le professeur**  
- Deux bibliothèques PDF : OpenHTML-to-PDF (principal), iText/OpenPDF (secours).  
- Le contenu (titres, valeurs) est construit dans le contrôleur ; le générateur assemble le tout et gère l’export fichier.

---

### 2.5 Workflow Paiement (avance projet) — Stripe + BDD + WhatsApp

**À quoi ça sert ?**  
Permettre de « payer » un avance projet : saisie bénéficiaire, montant, référence projet, carte test → appel Stripe → enregistrement en base → envoi WhatsApp de confirmation.

**Où c’est ?**
- **Vue** : `finance/views/paiement.fxml`
- **Contrôleur** : `PaiementController`
- **Services** : `StripePaymentService`, `PaiementService`, `SmsService`
- **Modèle** : `Paiement` (montant, date, statut, stripePaymentId, referenceProjet, nomBeneficiaire)

**Flux complet (à savoir pour la validation)**  

1. **Saisie** : bénéficiaire (ComboBox chargé depuis la table `users`), montant, référence projet, numéro de carte, expiration, CVV.  
2. **Validation** : champs non vides, montant > 0, date d’expiration et CVV valides.  
3. **Traitement en arrière-plan** : une `Task` (JavaFX) exécutée dans un thread séparé pour ne pas bloquer l’interface :  
   - `StripePaymentService.resolvePaymentMethodId(...)` → obtention du Payment Method ID  
   - `StripePaymentService.createPaymentIntent(montantCentimes)` → création du PaymentIntent  
   - `StripePaymentService.confirmPayment(intentId, pmId)` → confirmation  
4. **Si succès Stripe** :  
   - Création d’un objet `Paiement` (montant, date, SUCCESS, intentId, référence, bénéficiaire).  
   - `PaiementService.ajouterPaiement(p)` → enregistrement en base (table `paiement`, avec `stripe_payment_id`).  
   - `SmsService.getInstance().sendPaymentSuccess(...)` → envoi du message WhatsApp (sans bloquer la validation du paiement en cas d’échec SMS).  
5. **UI** : message de succès, fenêtre de détail (montant, bénéficiaire, date, ID transaction Stripe).

**Points pour le professeur**  
- Métier avancé = chaîne complète : formulaire → Stripe → BDD → notification.  
- Thread dédié pour Stripe pour garder l’interface réactive.  
- Persistance du `stripe_payment_id` pour traçabilité.

---

## PARTIE 3 — RÉCAPITULATIF POUR RÉPONDRE AU PROFESSEUR

| Sujet | Réponse courte |
|-------|----------------|
| **Quelles API externes ?** | **Stripe** (paiements carte, mode TEST) et **Twilio** (envoi WhatsApp). Les deux utilisent des appels HTTP ; les clés sont dans `config.properties`. |
| **Où sont les clés API ?** | `src/main/resources/config.properties` (Stripe, Twilio, et **OpenAI** : `openai.api.key` pour le chatbot IA). |
| **Comment fonctionne le chatbot ?** | Si `openai.api.key` est configurée : réponses par **IA (OpenAI GPT)** avec contexte (dernier bulletin, liste employés). Sinon (ou en erreur) : **repli par règles** + mots-clés (finance/paie), refus hors-sujet, personnalisation via le même contexte. |
| **Résumé automatique PDF ?** | Méthode `buildProfessionalSummary` dans `FinanceController` : agrège contrat, bulletins, primes, comptes pour l’employé et produit un paragraphe. Injecté comme `customSummary` dans le générateur PDF. Pas d’IA externe. |
| **Calculs fiscaux ?** | `TaxCalculationService` : CNSS (9,18 % / 16,5 %), IRPP progressif (tranches 0–35 %). Tout en Java, pas d’API. |
| **Génération PDF ?** | `PDFGenerator` : HTML à partir d’un template + sections ; conversion en PDF (OpenHTML-to-PDF, puis iText en secours). Données et résumé fournis par `FinanceController`. |
| **Workflow paiement ?** | Formulaire → Stripe (resolve PM, create PaymentIntent, confirm) → sauvegarde en BDD (`Paiement` + `stripe_payment_id`) → envoi WhatsApp (Twilio). Task en arrière-plan pour ne pas bloquer l’UI. |

---

## PARTIE 4 — FICHIERS À CITER PAR THÈME

- **Stripe** : `StripePaymentService.java`, `config.properties` (stripe.*), `PaiementController.java`  
- **Twilio** : `SmsService.java`, `config.properties` (twilio.*), appel depuis `PaiementController` après succès paiement  
- **Chatbot (avec IA)** : `FinanceChatbotService.java`, `FinanceChatbotAIService.java` (appel OpenAI), `config.properties` (openai.api.key), `EmployeurFinanceController.java` (buildChatContext, handleChatSend), `EmployeurFinanceView.fxml`  
- **Résumé PDF** : `FinanceController.buildProfessionalSummary`, `handleGenerateEmployeeReport`, `PDFGenerator.generateEmployeeReport` (paramètre customSummary)  
- **Fiscal** : `TaxCalculationService.java`  
- **PDF** : `PDFGenerator.java`, `rapport_financier_template.html`, `FinanceController` (buildContractInfo, buildBankInfo, buildBonusInfo, buildPayslipInfo)  
- **Paiement (workflow)** : `PaiementController.java`, `PaiementService.java`, `Paiement.java`, `paiement.fxml`

Avec ce guide, tu peux expliquer toutes les API et tous les métiers avancés de la gestion finance et répondre de façon précise aux questions du professeur.
