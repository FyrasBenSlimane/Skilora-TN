# 🚀 MODIFICATIONS APPLIQUÉES - FINANCE MODULE

## ✅ DÉCISION FINALE

**Utiliser `User.java` existant au lieu d'`EmployeeRow.java`**

---

## 📋 MODIFICATIONS À FAIRE

### 1️⃣ MODIFIER LES IMPORTS dans FinanceController.java

**REMPLACER** ligne 14 :
```java
import com.skilora.finance.model.*;
```

**PAR** :
```java
import com.skilora.finance.model.ContractRow;
import com.skilora.finance.model.BankAccountRow;
import com.skilora.finance.model.BonusRow;
import com.skilora.finance.model.PayslipRow;
import com.skilora.model.entity.User;
import com.skilora.model.service.UserService;
```

### 2️⃣ REMPLACER tous les `EmployeeRow` par `User`

**Chercher/Remplacer dans FinanceController.java** :
- `EmployeeRow` → `User`
- `emp.getFullName()` → `emp.getFullName()` (OK, existe déjà)
- `new EmployeeRow(...)` → charger depuis la base

### 3️⃣ SUPPRIMER la section Employees

**Dans FinanceController.java, SUPPRIMER** :
- Toutes les variables `employee_xxxField`
- Toutes les méthodes `handleAddEmployee()`, `handleUpdateEmployee()`, etc.
- La méthode `initializeEmployeeTab()`
- L'ObservableList `employeeData`

### 4️⃣ MODIFIER initialize()

**REMPLACER** :
```java
public void initialize(URL location, ResourceBundle resources) {
    initializeEmployeeTab();  // ❌ SUPPRIMER
    initializeContractTab();
    ...
    loadSampleData();  // ❌ REMPLACER
}
```

**PAR** :
```java
public void initialize(URL location, ResourceBundle resources) {
    initializeContractTab();
    initializeBankTab();
    initializeBonusTab();
    initializePayslipTab();
    initializeReportsTab();
    
    loadUsersFromDatabase();  // ✅ NOUVEAU
}
```

### 5️⃣ AJOUTER méthode loadUsersFromDatabase()

```java
private void loadUsersFromDatabase() {
    UserService userService = new UserService();
    List<User> users = userService.getAllUsers();
    
    ObservableList<User> userList = FXCollections.observableArrayList(users);
    
    // Remplir tous les ComboBox
    contract_userIdCombo.setItems(userList);
    bank_userIdCombo.setItems(userList);
    bonus_userIdCombo.setItems(userList);
    payslip_userIdCombo.setItems(userList);
    report_employeeCombo.setItems(userList);
}
```

### 6️⃣ SUPPRIMER EmployeeRow.java

**Fichier à supprimer** :
```
src/main/java/com/skilora/finance/model/EmployeeRow.java
```

### 7️⃣ SUPPRIMER l'onglet Employees du FXML

**Dans FinanceView.fxml**, supprimer le Tab Employees (environ lignes 50-150)

---

## ⚡ VERSION RAPIDE

Vu que c'est long, je vais créer un script de remplacement automatique.

Voulez-vous que je :
**A) Fasse les modifications manuellement (précis mais long)**
**B) Crée un nouveau FinanceController.java simplifié (rapide)**

Choisissez A ou B !
