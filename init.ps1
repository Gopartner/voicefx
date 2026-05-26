# VoiceFX - Project Initialization Script
# Run this to set up Gradle wrapper and build the project

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== VoiceFX Project Initialization ===" -ForegroundColor Cyan
Write-Host ""

# Check for Java
$javaVersion = java -version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Java not found. Install JDK 17 and set JAVA_HOME." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Java detected: $javaVersion" -ForegroundColor Green

# Check for Gradle wrapper jar
$wrapperJar = Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "Downloading Gradle wrapper JAR..." -ForegroundColor Yellow
    $wrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar -UseBasicParsing
        Write-Host "[OK] Gradle wrapper downloaded" -ForegroundColor Green
    } catch {
        Write-Host "WARNING: Could not download wrapper JAR." -ForegroundColor Yellow
        Write-Host "Please open the project in Android Studio - it will set up the wrapper automatically." -ForegroundColor Yellow
    }
} else {
    Write-Host "[OK] Gradle wrapper JAR found" -ForegroundColor Green
}

# Check local.properties
$localProps = Join-Path $ProjectRoot "local.properties"
if (-not (Test-Path $localProps)) {
    Write-Host ""
    Write-Host "WARNING: local.properties not found." -ForegroundColor Yellow
    Write-Host "Copy local.properties.example to local.properties and fill in your GitHub credentials." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To build the project:" -ForegroundColor White
Write-Host "  .\gradlew assembleDebug" -ForegroundColor Gray
Write-Host ""
Write-Host "To install on device:" -ForegroundColor White
Write-Host "  .\gradlew installDebug" -ForegroundColor Gray
Write-Host ""
Write-Host "Or open the project in Android Studio and click Run." -ForegroundColor White
