$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$content = Get-Content $file -Raw
$content = $content -replace "TLComboBox<EmployeeRow>", "TLComboBox<String>"
$content = $content -replace "import com.skilora.finance.model.EmployeeRow;", ""
Set-Content $file $content
Write-Host "Replaced EmployeeRow with String in ComboBoxes."
