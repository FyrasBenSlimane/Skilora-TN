$file = "src\main\java\com\skilora\finance\controller\FinanceController.java"
$lines = Get-Content $file
$index = 0
for($i=0; $i -lt $lines.Count; $i++) {
    if ($lines[$i].Trim() -eq "private double parseDouble(String text, double defaultValue) {") {
        $index = $i
    }
}
Write-Host "Found parseDouble at $index"
# La méthode fait :
# private double parseDouble... { (0)
#   try { (1)
#     return ... (2)
#   } catch ... { (3)
#     return ... (4)
#   } (5)
# } (6)
$endIndex = $index + 6
$linesContent = $lines[$endIndex]
Write-Host "Content at endIndex: $linesContent"
# On garde jusqu'à endIndex inclus, puis on ajoute l'accolade de classe
$newLines = $lines[0..$endIndex] + "}"
$newLines | Set-Content $file
Write-Host "File truncated correctly."
