$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$lines = Get-Content $file
$newLines = $lines[0..26] + $lines[60..($lines.Count-1)]
$newLines | Set-Content $file
Write-Host "Done! Deleted lines 27-59"
