# Fix JAVA_HOME System Variable
# Run this script as Administrator in PowerShell

Write-Host "=== Fixing JAVA_HOME System Variable ===" -ForegroundColor Green
Write-Host ""

# Show current value
$currentJavaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
Write-Host "Current JAVA_HOME (System): $currentJavaHome" -ForegroundColor Yellow

# Correct value (remove \bin if present)
$correctJavaHome = 'C:\Program Files\Android\Android Studio\jbr'

# Set the correct value
Write-Host ""
Write-Host "Setting JAVA_HOME to: $correctJavaHome" -ForegroundColor Cyan
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', $correctJavaHome, 'Machine')

# Verify
$newJavaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
Write-Host ""
Write-Host "New JAVA_HOME (System): $newJavaHome" -ForegroundColor Green

# Check if \bin is in PATH and suggest removal
Write-Host ""
Write-Host "Checking PATH variable..." -ForegroundColor Cyan
$path = [System.Environment]::GetEnvironmentVariable('Path', 'Machine')

if ($path -like "*\jbr\bin\bin*") {
    Write-Host "WARNING: PATH contains '\jbr\bin\bin' - this will cause issues!" -ForegroundColor Red
    Write-Host "Please manually remove duplicate \bin from PATH in System Environment Variables" -ForegroundColor Yellow
} elseif ($path -like "*%JAVA_HOME%\bin*") {
    Write-Host "OK: PATH contains '%JAVA_HOME%\bin' reference" -ForegroundColor Green
} else {
    Write-Host "INFO: Consider adding '%JAVA_HOME%\bin' to PATH" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== IMPORTANT ===" -ForegroundColor Red
Write-Host "1. Close ALL open terminals (Git Bash, PowerShell, CMD)" -ForegroundColor Yellow
Write-Host "2. Open a NEW terminal" -ForegroundColor Yellow
Write-Host "3. Test with: java -version" -ForegroundColor Yellow
Write-Host ""
Write-Host "Done!" -ForegroundColor Green
