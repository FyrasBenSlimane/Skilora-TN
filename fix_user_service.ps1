$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$content = Get-Content $file -Raw
$newContent = $content -replace "new UserService\(\)", "UserService.getInstance()"
Set-Content $file $newContent
Write-Host "Replaced new UserService() with UserService.getInstance()"
