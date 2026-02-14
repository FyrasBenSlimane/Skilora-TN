$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$part2 = Get-Content "part2.java" -Raw
$lines = Get-Content $file

# 1. Truncate after toggleTheme
$index = 0
for($i=0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match "private void toggleTheme") {
        $index = $i
        break
    }
}
$cutIndex = $index + 5
$part1Lines = $lines[0..$cutIndex]
$part1 = $part1Lines -join "`n"

# 2. Replacements in part1
$part1 = $part1 -replace "EmployeeRow", "String"

# Regex for method calls replacement
# emp.getId() -> extractUserId(emp)
$part1 = [regex]::Replace($part1, "([a-zA-Z0-9_]+)\.getId\(\)", "extractUserId(`$1)")
# emp.getFullName() -> extractUserName(emp)
$part1 = [regex]::Replace($part1, "([a-zA-Z0-9_]+)\.getFullName\(\)", "extractUserName(`$1)")

# findEmployeeById call
$part1 = $part1 -replace "findEmployeeById", "getEmployeeStringById"

# Ensure User variables are now String
$part1 = $part1 -replace "User emp =", "String emp ="
$part1 = $part1 -replace "User user =", "String user ="
$part1 = $part1 -replace "\(User\)", "(String)"

# 3. Concatenate
$final = $part1 + "`n" + $part2

Set-Content $file $final
Write-Host "FinanceController refactored to String usage only."
