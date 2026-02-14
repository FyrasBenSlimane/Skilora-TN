# Script PowerShell pour nettoyer FinanceController.java
# Supprime toutes les références à Employee/User management

$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$content = Get-Content $file -Raw

# Supprimer les variables employee_xxx
$content = $content -replace '(?s)    // Employee Management.*?private User selectedEmployee = null;\r?\n\r?\n', ''

# Supprimer les imports inutiles de User si employeeData n'existe plus
# (on garde User pour les ComboBox)

# Sauvegarder
$content | Set-Content $file -NoNewline

Write-Host "✅ Section Employee supprimée !"
Write-Host "Vérifiez le fichier FinanceController.java"
