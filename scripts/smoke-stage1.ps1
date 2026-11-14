#requires -Version 5.1
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

function Stop-Check([string]$Message) {
    Write-Host "STOP: $Message" -ForegroundColor Red
    exit 1
}

function Http200([string]$Url) {
    $last = ''
    for ($attempt = 1; $attempt -le 12; $attempt++) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
            if ([int]$r.StatusCode -eq 200) {
                Write-Host "PASS HTTP 200: $Url" -ForegroundColor Green
                return
            }
            $last = "HTTP $($r.StatusCode)"
        } catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 5
    }
    Stop-Check "Failed HTTP request to $Url : $last"
}

function Http404([string]$Url) {
    $code = 0
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
        $code = [int]$r.StatusCode
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            Stop-Check "Failed HTTP request to $Url : $($_.Exception.Message)"
        }
        $code = [int]$response.StatusCode
    }
    if ($code -ne 404) {
        Stop-Check "Unexpected route exposure: $Url returned HTTP $code (expected 404)."
    }
    Write-Host "PASS HTTP 404 (not routed): $Url" -ForegroundColor Green
}

function Registry() {
    $r = Invoke-WebRequest -Uri 'http://localhost:8761/eureka/apps' -UseBasicParsing -Headers @{ Accept='application/json' } -TimeoutSec 10
    return ($r.Content | ConvertFrom-Json)
}

function Registered([object]$Registry, [string]$Id) {
    $apps = @($Registry.applications.application)
    foreach ($app in $apps) {
        if ([string]$app.name -ine $Id) { continue }
        foreach ($item in @($app.instance)) {
            if ([string]$item.status -ieq 'UP') { return $item }
        }
    }
    return $null
}

function InstancePort([object]$Instance) {
    if ($null -eq $Instance) { return 0 }
    $port = $Instance.port
    if ($port -is [int] -or $port -is [long] -or $port -is [string]) { return [int]$port }
    $value = $port.PSObject.Properties['$']
    if ($null -eq $value) { return 0 }
    return [int]$value.Value
}

function RemoteConfig([string]$Name, [int]$Port) {
    $endpoint = "http://config-server:$Port/$Name/default"
    $result = & docker compose exec -T discovery-server sh -c "wget -q -T 10 -O - '$endpoint'" 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Config Server unreachable: $endpoint : $($result -join ' ')" }
    $text = ($result -join "`n")
    return ($text | ConvertFrom-Json)
}

function ConfigProp([object]$Config, [string]$Key) {
    foreach ($source in @($Config.propertySources)) {
        if ($null -eq $source.source) { continue }
        $entry = $source.source.PSObject.Properties[$Key]
        if ($null -ne $entry) { return [string]$entry.Value }
    }
    return $null
}

if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml'))) { Stop-Check "No pom.xml at $repo" }
Push-Location $repo
try {
    & docker compose ps | Out-Host
    if ($LASTEXITCODE -ne 0) { Stop-Check 'Docker Compose is not available or services are not running.' }

    Http200 'http://localhost:8761/actuator/health'
    $ids = @('CONFIG-SERVER','MAIN-SERVICE','STATS-SERVER','GATEWAY-SERVER')
    $instances = @{}
    for ($i = 1; $i -le 12; $i++) {
        $r = Registry
        $instances.Clear()
        foreach ($id in $ids) {
            $inst = Registered $r $id
            if ($null -ne $inst -and (InstancePort $inst) -gt 0) { $instances[$id] = $inst }
        }
        if ($instances.Count -eq $ids.Count) { break }
        Start-Sleep -Seconds 5
    }
    foreach ($id in $ids) {
        if (-not $instances.ContainsKey($id)) { Stop-Check "Missing UP Eureka instance: $id" }
        $p = InstancePort $instances[$id]
        Write-Host "PASS Eureka: $id on port $p" -ForegroundColor Green
    }
    $configPort = InstancePort $instances['CONFIG-SERVER']
    if ($configPort -eq 8761 -or $configPort -eq 8080) { Stop-Check 'Config Server must use a random port.' }
    if ((InstancePort $instances['MAIN-SERVICE']) -eq 8080) { Stop-Check 'Main service must use a random port.' }
    if ((InstancePort $instances['STATS-SERVER']) -eq 9090) { Stop-Check 'Statistics server must use a random port.' }
    if ((InstancePort $instances['GATEWAY-SERVER']) -ne 8080) { Stop-Check 'Gateway must use port 8080.' }

    foreach ($name in @('main-service','stats-server','gateway-server')) {
        $config = RemoteConfig $name $configPort
        if ([string]$config.name -ne $name -or @($config.propertySources).Count -lt 1) {
            Stop-Check "Remote config for $name is missing or empty."
        }
        $port = ConfigProp $config 'server.port'
        $expected = if ($name -eq 'gateway-server') { '8080' } else { '0' }
        if ($port -ne $expected) { Stop-Check "Unexpected Config Server port for $name : $port (expected $expected)." }
        if ($name -eq 'gateway-server') {
            if ((ConfigProp $config 'spring.cloud.gateway.discovery.locator.enabled') -ne 'false') {
                Stop-Check 'Automatic Eureka routes must be disabled in Gateway.'
            }
            if ((ConfigProp $config 'spring.cloud.gateway.routes[0].uri') -ne 'lb://main-service') {
                Stop-Check 'Gateway route to lb://main-service is missing in Config Server.'
            }
        } else {
            if ([string]::IsNullOrWhiteSpace((ConfigProp $config 'spring.datasource.url'))) {
                Stop-Check "Database configuration for $name is missing from Config Server."
            }
        }
        Write-Host "PASS Config Server: $name, server.port=$port" -ForegroundColor Green
    }

    Http200 'http://localhost:8080/actuator/health'
    Http200 'http://localhost:8080/categories'
    Http200 'http://localhost:8080/events'
    Http404 'http://localhost:8080/config-server/actuator/health'
    Http404 'http://localhost:8080/stats-server/actuator/health'
    Http404 'http://localhost:8080/gateway-server/actuator/health'
    Write-Host 'PASS: Stage 1 smoke checks (Eureka, Config Server, Gateway) completed.' -ForegroundColor Green
} catch {
    Stop-Check $_.Exception.Message
} finally {
    Pop-Location
}
