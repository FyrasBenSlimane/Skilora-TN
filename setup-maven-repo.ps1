# Maven Repository Setup Script
# This script creates the Maven repository directory and verifies configuration

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "Maven Repository Setup" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Set the new repository path
$mavenRepo = "C:\m2-repo"

# Create the repository directory if it doesn't exist
Write-Host "Creating Maven repository directory: $mavenRepo" -ForegroundColor Yellow
if (-not (Test-Path $mavenRepo)) {
    try {
        New-Item -ItemType Directory -Path $mavenRepo -Force | Out-Null
        Write-Host "✓ Repository directory created successfully" -ForegroundColor Green
    } catch {
        Write-Host "✗ Failed to create repository directory: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✓ Repository directory already exists" -ForegroundColor Green
}

# Verify settings.xml exists
$settingsXml = "$env:USERPROFILE\.m2\settings.xml"
Write-Host ""
Write-Host "Checking Maven settings.xml: $settingsXml" -ForegroundColor Yellow
if (Test-Path $settingsXml) {
    Write-Host "✓ settings.xml exists" -ForegroundColor Green
    $content = Get-Content $settingsXml -Raw
    if ($content -match "C:/m2-repo") {
        Write-Host "✓ settings.xml contains correct repository path" -ForegroundColor Green
    } else {
        Write-Host "⚠ settings.xml exists but may not have correct path" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ settings.xml not found" -ForegroundColor Red
}

# Verify .mvn/maven.config exists
$mavenConfig = ".\.mvn\maven.config"
Write-Host ""
Write-Host "Checking project Maven config: $mavenConfig" -ForegroundColor Yellow
if (Test-Path $mavenConfig) {
    Write-Host "✓ .mvn/maven.config exists" -ForegroundColor Green
    $content = Get-Content $mavenConfig -Raw
    if ($content -match "C:/m2-repo") {
        Write-Host "✓ .mvn/maven.config contains correct repository path" -ForegroundColor Green
    }
} else {
    Write-Host "⚠ .mvn/maven.config not found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Open IntelliJ IDEA" -ForegroundColor White
Write-Host "2. Go to: File → Settings → Build, Execution, Deployment → Build Tools → Maven" -ForegroundColor White
Write-Host "3. Set 'Local repository' to: C:/m2-repo" -ForegroundColor White
Write-Host "4. Click Apply and OK" -ForegroundColor White
Write-Host "5. Restart IntelliJ IDEA" -ForegroundColor White
Write-Host ""
Write-Host "See INTELLIJ_MAVEN_FIX.txt for detailed instructions." -ForegroundColor Cyan
