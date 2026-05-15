$ErrorActionPreference = "Continue"

$baseDir = $PSScriptRoot
$mvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd'

$services = @(
    "auth-service",
    "profile-service",
    "job-service",
    "application-service",
    "notification-service",
    "subscription-service",
    "interview-service",
    "analytics-service",
    "api-gateway"
)

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Running SonarQube Analysis & Coverage Verification" -ForegroundColor Cyan
Write-Host "Requirement: >80% Code Coverage" -ForegroundColor Yellow
Write-Host "========================================================`n" -ForegroundColor Cyan

$results = @()

foreach ($service in $services) {
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Analyzing $service..." -ForegroundColor Yellow
    $servicePath = Join-Path $baseDir $service
    
    if (Test-Path $servicePath) {
        Set-Location $servicePath
        
        # Run verify (includes tests and jacoco:check) and then sonar:sonar
        # We explicitly point to the jacoco.xml report and exclude boilerplate from coverage
        & $mvn clean verify sonar:sonar "-Dsonar.login=admin" "-Dsonar.password=Kundu@70677" "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml" "-Dsonar.scm.disabled=true" "-Dsonar.coverage.exclusions=**/dto/**,**/entity/**,**/config/**,**/security/**,**/util/**,**/*Application.*"
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "FAILED: $service failed verification or analysis." -ForegroundColor Red
            $results += [PSCustomObject]@{ Service = $service; Status = "Failed" }
        } else {
            Write-Host "SUCCESS: $service passed verification." -ForegroundColor Green
            $results += [PSCustomObject]@{ Service = $service; Status = "Passed" }
        }
    } else {
        Write-Host "SKIPPED: $service directory not found." -ForegroundColor Gray
    }
}

Set-Location $baseDir

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "Summary of Analysis" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$results | Format-Table -AutoSize

$failed = $results | Where-Object { $_.Status -eq "Failed" }
if ($failed.Count -gt 0) {
    Write-Host "Some services failed to meet the 80% coverage requirement or Sonar analysis failed." -ForegroundColor Red
    exit 1
} else {
    Write-Host "All services passed the 80% coverage requirement and Sonar analysis!" -ForegroundColor Green
}
