# 🔧 RÉSOLUTION DES ERREURS - MODULE FINANCE

## ❌ Erreur: "Error loading Finance Module: FinanceView.fxml:46"

### Problème
L'application affiche une erreur lors du chargement du module Finance, pointant vers la ligne 46 du fichier FXML.

### Cause
Le composant `TLComboBox` n'avait pas l'attribut `promptText` nécessaire pour FXML.

### ✅ Solution Appliquée
Ajout des méthodes `getPromptText()` et `setPromptText()` dans `TLComboBox.java`.

### Vérification
1. Ouvrez le fichier : `src/main/java/com/skilora/framework/components/TLComboBox.java`
2. Vérifiez que ces méthodes existent :
   ```java
   public String getPromptText() {
       return comboBox.getPromptText();
   }

   public void setPromptText(String promptText) {
       comboBox.setPromptText(promptText);
   }
   ```

---

## 🛠️ ERREURS COMMUNES ET SOLUTIONS

### Erreur 1: "Cannot find symbol TLComboBox"
**Cause**: Le projet n'est pas compilé ou les classes sont manquantes.

**Solution**:
```powershell
mvn clean compile
```

### Erreur 2: "javafx.fxml.LoadException"
**Cause**: Erreur dans le fichier FXML (balise incorrecte, attribut manquant, etc.)

**Solution**:
1. Vérifiez le numéro de ligne indiqué dans l'erreur
2. Assurez-vous que tous les attributs FXML ont des getters/setters dans le composant
3. Recompilez : `mvn clean compile`

### Erreur 3: "NullPointerException in FinanceController"
**Cause**: Un composant FXML n'est pas lié correctement avec `fx:id`.

**Solution**:
1. Vérifiez que chaque composant dans FXML a un `fx:id`
2. Vérifiez que le contrôleur a un champ `@FXML` correspondant
3. Les noms doivent correspondre exactement

### Erreur 4: "Communications link failure" (Base de données)
**Cause**: MySQL n'est pas démarré ou la connexion échoue.

**Solution**:
```powershell
# Démarrer MySQL avec XAMPP
.\START_MYSQL.ps1
```

L'application fonctionne aussi en mode OFFLINE (sans base de données).

---

## 🚀 COMMANDES UTILES

### Compiler le projet
```powershell
mvn clean compile
```

### Lancer l'application
```powershell
mvn javafx:run
```

### Compiler et lancer
```powershell
mvn clean compile javafx:run
```

### Nettoyer complètement
```powershell
mvn clean
```

### Compiler sans tests
```powershell
mvn clean compile -DskipTests
```

---

## 📋 CHECKLIST DE DÉPANNAGE

Avant de demander de l'aide, vérifiez :

- [ ] Java 17+ est installé : `java -version`
- [ ] Maven est installé : `mvn -version`
- [ ] Le projet compile sans erreur : `mvn clean compile`
- [ ] Tous les fichiers FXML sont dans `src/main/resources/fxml/`
- [ ] Tous les contrôleurs sont dans `src/main/java/com/skilora/.../controller/`
- [ ] Les `fx:id` dans FXML correspondent aux champs `@FXML` dans le contrôleur

---

## 🔍 DIAGNOSTIQUER UNE ERREUR FXML

### Étape 1: Identifier la ligne
L'erreur indique le numéro de ligne, par exemple :
```
Error loading Finance Module: FinanceView.fxml:46
                                               ^^^
                                            Ligne 46
```

### Étape 2: Ouvrir le fichier
```
src/main/resources/fxml/FinanceView.fxml
```

### Étape 3: Vérifier la ligne
Allez à la ligne 46 et vérifiez :
- Le nom de la balise est-il correct ?
- L'attribut existe-t-il dans le composant Java ?
- Y a-t-il une faute de frappe ?

### Étape 4: Vérifier le composant Java
Si c'est un `TLComboBox`, vérifiez que :
```java
src/main/java/com/skilora/framework/components/TLComboBox.java
```
contient des méthodes getter/setter pour chaque attribut FXML.

**Exemple**:
```xml
<!-- FXML -->
<TLComboBox fx:id="myCombo" label="Choose" promptText="Select..."/>
```

Nécessite dans `TLComboBox.java`:
```java
public String getLabel() { ... }
public void setLabel(String label) { ... }
public String getPromptText() { ... }
public void setPromptText(String promptText) { ... }
```

---

## 🔄 SI LE PROBLÈME PERSISTE

### Nettoyage complet
```powershell
# 1. Nettoyer Maven
mvn clean

# 2. Supprimer le cache (si nécessaire)
Remove-Item -Recurse -Force target

# 3. Recompiler
mvn compile

# 4. Lancer
mvn javafx:run
```

### Vérifier les logs
Les erreurs détaillées apparaissent dans la console. Cherchez :
- `Caused by:` - La cause racine de l'erreur
- `at ligne X` - Le numéro de ligne exacte
- Stack trace - La trace complète de l'erreur

### Rebuild dans IntelliJ
Si vous utilisez IntelliJ IDEA :
1. **Build** → **Rebuild Project**
2. **Run** → **Run 'FinanceApp'**

---

## 💡 CONSEILS DE DÉVELOPPEMENT

### Toujours compiler avant de lancer
```powershell
mvn clean compile && mvn javafx:run
```

### Utiliser les scripts fournis
- `COMPILE_AND_RUN.bat` - Compile et propose de lancer
- `RUN_FINANCE.bat` - Lance directement (si déjà compilé)

### En cas de doute
Supprimez le dossier `target/` et recompilez tout :
```powershell
Remove-Item -Recurse -Force target
mvn clean compile javafx:run
```

---

## 📞 SUPPORT

Si l'erreur persiste après avoir suivi ces étapes :

1. Notez le **message d'erreur complet**
2. Notez le **numéro de ligne** du fichier FXML
3. Vérifiez les **fichiers récemment modifiés**
4. Consultez les logs complets dans la console

---

## ✅ VÉRIFICATION RAPIDE

Pour vérifier que tout fonctionne :

```powershell
# Test rapide
.\COMPILE_AND_RUN.bat
```

Si ça compile et lance sans erreur : ✅ **Tout est OK !**

---

**Date**: 11 février 2026  
**Version**: 2.0.0  
**Status**: ✅ Problème résolu
