# 📂 Structure du Projet - Gestion de Paie

## 🔑 Identifiants de connexion

| Rôle       | Username  | Password  | Description                          |
|-----------|-----------|-----------|--------------------------------------|
| ADMIN     | admin     | admin123  | Gestion complète (CRUD)              |
| USER      | user      | user123   | Consultation de toutes les données   |
| EMPLOYER  | employer  | emp123    | Consultation de sa propre paie       |

---

## 🏗️ Architecture 3 Espaces

### 1. 💼 FINANCE (Admin) - `FinanceController.java` + `FinanceView.fxml`
L'administrateur peut **gérer tout** :
- ✅ Ajouter / Modifier / Supprimer des contrats
- ✅ Ajouter / Modifier / Supprimer des comptes bancaires
- ✅ Ajouter / Modifier / Supprimer des primes (bonus)
- ✅ Ajouter / Modifier / Supprimer des bulletins de paie
- ✅ Calcul de taxes (CNSS, IRPP)
- ✅ Export PDF

### 2. 👁️ USER (Consultation Admin) - `UserFinanceController.java` + `UserFinanceView.fxml`
Le USER peut **consulter** tout ce que l'admin a fait :
- 👁️ Voir tous les contrats
- 👁️ Voir tous les comptes bancaires
- 👁️ Voir toutes les primes
- 👁️ Voir tous les bulletins de paie
- 🔒 **Aucune modification possible** (lecture seule)
- 📊 Résumé avec compteurs (nombre d'employés, contrats, etc.)

### 3. 👤 EMPLOYEUR (Ma Paie) - `EmployeurFinanceController.java` + `EmployeurFinanceView.fxml`
L'employeur peut consulter **uniquement ses propres données** :
- 💰 Son salaire actuel
- 🎁 Ses primes (bonus)
- 📋 Calcul CNSS (9.18%)
- 📋 Calcul IRPP (barème progressif tunisien)
- 📋 Détail des déductions
- ✅ Son salaire net
- 📄 Ses bulletins de paie
- 🏦 Ses comptes bancaires
- 📋 Ses contrats
- 🔒 **Aucune modification possible** (lecture seule)

---

## 📁 Fichiers Clés

```
src/main/java/com/skilora/
├── finance/
│   ├── controller/
│   │   ├── FinanceController.java        ← ADMIN (CRUD complet)
│   │   ├── UserFinanceController.java    ← USER (lecture seule, tout voir)
│   │   └── EmployeurFinanceController.java ← EMPLOYEUR (lecture seule, ma paie)
│   ├── model/
│   │   ├── ContractRow.java
│   │   ├── BankAccountRow.java
│   │   ├── BonusRow.java
│   │   └── PayslipRow.java
│   ├── service/
│   │   ├── FinanceService.java           ← Queries DB (+ per-user queries)
│   │   ├── TaxCalculationService.java     ← Calculs CNSS/IRPP
│   │   ├── PayslipService.java
│   │   └── ...
│   └── ...
├── ui/
│   └── MainView.java                     ← Navigation par rôle
└── ...

src/main/resources/fxml/
├── FinanceView.fxml                      ← Vue ADMIN
├── UserFinanceView.fxml                  ← Vue USER
└── EmployeurFinanceView.fxml             ← Vue EMPLOYEUR
```

---

## 🔄 Flux de Navigation

```
Login → Vérification du rôle → Sidebar selon le rôle

ADMIN  → Sidebar avec "Finance"              → FinanceView.fxml (CRUD)
USER   → Sidebar avec "Consultation Finance"  → UserFinanceView.fxml (Lecture)
EMPLOYER → Sidebar avec "Ma Paie"            → EmployeurFinanceView.fxml (Lecture)
```

---

## 📊 Calculs de Paie (Tunisie)

| Composant         | Taux     | Description                        |
|-------------------|----------|------------------------------------|
| CNSS Employé      | 9.18%    | Part salariale                     |
| CNSS Employeur    | 16.5%    | Part patronale                     |
| IRPP              | Progressif| Barème tunisien 2025              |
| 0-5000 TND        | 0%       |                                    |
| 5001-20000 TND    | 26%      |                                    |
| 20001-30000 TND   | 28%      |                                    |
| 30001-50000 TND   | 32%      |                                    |
| 50001+ TND        | 35%      |                                    |
