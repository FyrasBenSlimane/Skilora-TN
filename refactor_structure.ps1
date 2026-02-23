$source = "c:\Users\21625\Downloads\JAVAFX11\JAVAFX\src\main\java\com\skilora\controller"
$target = "c:\Users\21625\Downloads\JAVAFX11\JAVAFX\src\main\java\com\skilora\recruitment\controller"
$fxml_dir = "c:\Users\21625\Downloads\JAVAFX11\JAVAFX\src\main\resources\com\skilora\view"

# Create Target Directory
New-Item -ItemType Directory -Force -Path $target | Out-Null

$files = @(
    "ActiveOffersController.java",
    "ApplicationInboxController.java",
    "ApplicationsController.java",
    "FeedController.java",
    "FormationsController.java",
    "InterviewsController.java",
    "JobDetailsController.java",
    "MyOffersController.java",
    "PostJobController.java",
    "ProfileWizardController.java"
)

Write-Host "Moving Controllers..."

foreach ($file in $files) {
    $src = Join-Path $source $file
    $dst = Join-Path $target $file
    
    if (Test-Path $src) {
        $content = Get-Content $src -Raw
        $newContent = $content -replace "package com.skilora.controller;", "package com.skilora.recruitment.controller;"
        Set-Content $dst $newContent -Encoding UTF8
        Remove-Item $src
        Write-Host "Moved $file"
    } else {
        Write-Host "Skipping $file (Not found)"
    }
}

Write-Host "Updating FXML Files..."

$fxmls = Get-ChildItem $fxml_dir -Filter *.fxml
foreach ($fxml in $fxmls) {
    $content = Get-Content $fxml.FullName -Raw
    $originalContent = $content
    
    foreach ($file in $files) {
        $class = $file.Replace(".java", "")
        $old = "com.skilora.controller.$class"
        $new = "com.skilora.recruitment.controller.$class"
        $content = $content.Replace($old, $new)
    }
    
    if ($content -ne $originalContent) {
        Set-Content $fxml.FullName $content -Encoding UTF8
        Write-Host "Updated $($fxml.Name)"
    }
}

Write-Host "Refactoring Complete!"
