$baseDir = "c:\Users\Kundu\Desktop\HireConnect\Backend\HireConnect-Platform-Backend\microservice-architecture"

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

foreach ($service in $services) {
    Write-Host "Starting $service..."
    $servicePath = Join-Path $baseDir $service
    $envPath = Join-Path $baseDir ".env"
    $command = "Get-Content '$envPath' | ForEach-Object { if (`$_ -match '^([^=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1], `$matches[2], 'Process') } }; cd '$servicePath'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd' spring-boot:run"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $command
    Start-Sleep -Seconds 5
}

Write-Host "All backend services are starting in separate windows."
