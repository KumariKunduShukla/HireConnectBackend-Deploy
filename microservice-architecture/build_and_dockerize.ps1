$ErrorActionPreference = "Stop"

$baseDir = $PSScriptRoot
$services = @(
    "discovery-server",
    "api-gateway",
    "auth-service",
    "profile-service",
    "job-service",
    "application-service",
    "notification-service",
    "subscription-service",
    "interview-service",
    "analytics-service"
)

Write-Host "Killing any running local Java and Node processes..." -ForegroundColor Cyan
Stop-Process -Name java -Force -ErrorAction SilentlyContinue
Stop-Process -Name node -Force -ErrorAction SilentlyContinue

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "Compiling all Microservices into .jar files..." -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

foreach ($service in $services) {
    Write-Host "Building $service..." -ForegroundColor Yellow
    $servicePath = Join-Path $baseDir $service
    Set-Location $servicePath
    
    # Run maven package skipping test compilation to generate the jar quickly
    & '.\mvnw.cmd' clean package "-Dmaven.test.skip=true"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build $service. Exiting." -ForegroundColor Red
        exit 1
    }
}

Set-Location $baseDir

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "All services built! Spinning up Docker Compose..." -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

docker-compose up --build -d

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "Success! The platform is booting up in Docker." -ForegroundColor Green
Write-Host "View backend services in Docker Desktop." -ForegroundColor Green
Write-Host "The Frontend is available at http://localhost" -ForegroundColor Green
Write-Host "========================================================`n" -ForegroundColor Green
