<#
.SYNOPSIS
  EventZone backend smoke test — Windows PowerShell native version.

.DESCRIPTION
  Builds the app, starts it, and walks through the full happy-path flow via HTTP
  (register/login, browse events, book a ticket, cancel it, organiser + admin
  actions), asserting the HTTP status code at every step. This is the Windows
  equivalent of scripts/smoke-test.sh (which needs bash/WSL/Git Bash to run) —
  use this one if you're in plain Windows PowerShell.

.EXAMPLE
  .\scripts\smoke-test.ps1
  # if PowerShell blocks script execution (common on a fresh machine):
  powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
#>

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$baseUrl = "http://localhost:8080"
$logFile = Join-Path $env:TEMP "eventzone-smoke-test.log"
$script:pass = 0
$script:fail = 0
$javaProc = $null

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [string]$Token = $null
    )
    $headers = @{}
    if ($Body) { $headers["Content-Type"] = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    try {
        if ($Body) {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -Body $Body -UseBasicParsing -TimeoutSec 15
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -UseBasicParsing -TimeoutSec 15
        }
        return [PSCustomObject]@{ StatusCode = [int]$resp.StatusCode; Content = $resp.Content }
    } catch {
        $ex = $_.Exception
        $statusCode = 0
        $content = ""
        if ($ex.Response) {
            try {
                # Windows PowerShell 5.1: System.Net.WebException / HttpWebResponse
                $statusCode = [int]$ex.Response.StatusCode
                $stream = $ex.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
            } catch {
                try {
                    # PowerShell 7+: HttpResponseException / HttpResponseMessage
                    $statusCode = [int]$ex.Response.StatusCode
                    $content = $ex.Response.Content.ReadAsStringAsync().Result
                } catch { }
            }
        }
        return [PSCustomObject]@{ StatusCode = $statusCode; Content = $content }
    }
}

function Test-Check {
    param([string]$Desc, [int]$Expected, [int]$Actual)
    if ($Actual -eq $Expected) {
        Write-Host "  [PASS] $Desc (HTTP $Actual)" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "  [FAIL] $Desc (expected HTTP $Expected, got $Actual)" -ForegroundColor Red
        $script:fail++
    }
}

function Get-Field {
    param([string]$Json, [string]$Key)
    try {
        $obj = $Json | ConvertFrom-Json
        return $obj.$Key
    } catch { return $null }
}

function Get-FirstId {
    param([string]$Json)
    try {
        $arr = $Json | ConvertFrom-Json
        if ($arr -and $arr.Count -gt 0) { return $arr[0].id }
        return $null
    } catch { return $null }
}

$script:exitCode = 0

try {
    Write-Host "== 1. Building (mvn -q -DskipTests package) ==" -ForegroundColor Cyan
    $mvn = & (Join-Path $PSScriptRoot "ensure-maven.ps1")
    if (-not $mvn) {
        Write-Host "Could not resolve a Maven executable — see errors above." -ForegroundColor Red
        $script:exitCode = 1
        return
    }
    & $mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed — fix compile errors before running the smoke test. See Maven output above." -ForegroundColor Red
        $script:exitCode = 1
        return
    }

    $jar = Get-ChildItem -Path (Join-Path $repoRoot "target") -Filter "eventzone-backend*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) {
        Write-Host "No jar found in target\ after build — aborting." -ForegroundColor Red
        $script:exitCode = 1
        return
    }

    Write-Host "== 2. Starting $($jar.Name) (log: $logFile) ==" -ForegroundColor Cyan
    if (Test-Path $logFile) { Remove-Item $logFile -Force }
    $javaProc = Start-Process -FilePath "java" -ArgumentList @("-jar", $jar.FullName) `
        -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" -PassThru -WindowStyle Hidden

    Write-Host "Waiting for the app to become ready on $baseUrl ..."
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        $r = Invoke-Api -Method GET -Url "$baseUrl/api/categories"
        if ($r.StatusCode -eq 200) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) {
        Write-Host "App did not become ready within 60s. Last log lines:" -ForegroundColor Red
        if (Test-Path $logFile) { Get-Content $logFile -Tail 40 }
        if (Test-Path "$logFile.err") { Get-Content "$logFile.err" -Tail 40 }
        $script:exitCode = 1
        return
    }
    Write-Host "App is up." -ForegroundColor Green
    Write-Host ""

    Write-Host "== 3. Walking the happy path ==" -ForegroundColor Cyan

    Write-Host "-- Auth --"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/auth/login" -Body '{"email":"admin@eventzone.com","password":"Password@123"}'
    Test-Check "Login as admin" 200 $r.StatusCode
    $adminToken = Get-Field $r.Content "token"

    $r = Invoke-Api -Method POST -Url "$baseUrl/api/auth/login" -Body '{"email":"organiser1@eventzone.com","password":"Password@123"}'
    Test-Check "Login as organiser" 200 $r.StatusCode
    $orgToken = Get-Field $r.Content "token"

    $rand = Get-Random
    $registerBody = "{`"email`":`"smoketest$rand@eventzone.com`",`"password`":`"Password@123`",`"name`":`"Smoke Test`"}"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/auth/register" -Body $registerBody
    Test-Check "Register new attendee" 201 $r.StatusCode

    $loginAttBody = "{`"email`":`"smoketest$rand@eventzone.com`",`"password`":`"Password@123`"}"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/auth/login" -Body $loginAttBody
    Test-Check "Login as newly-registered attendee" 200 $r.StatusCode
    $attendeeToken = Get-Field $r.Content "token"

    Write-Host "-- Categories & Events --"
    $r = Invoke-Api -Method GET -Url "$baseUrl/api/categories"
    Test-Check "List categories" 200 $r.StatusCode
    $categoryId = Get-FirstId $r.Content

    $r = Invoke-Api -Method GET -Url "$baseUrl/api/events"
    Test-Check "List events" 200 $r.StatusCode
    $eventId = Get-FirstId $r.Content

    $r = Invoke-Api -Method GET -Url "$baseUrl/api/events/$eventId"
    Test-Check "Get event detail" 200 $r.StatusCode
    $ticketCategoryId = $null
    try {
        $detail = $r.Content | ConvertFrom-Json
        $ticketCategoryId = $detail.ticketCategories[0].id
    } catch { }

    Write-Host "-- Organiser: create event + ticket category --"
    $createEventBody = "{`"title`":`"Smoke Test Concert`",`"description`":`"created by smoke-test.ps1`",`"eventDate`":`"2026-12-01T19:00:00`",`"venue`":`"Test Venue`",`"coverImageUrl`":`"https://example.com/x.jpg`",`"categoryId`":`"$categoryId`"}"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/events" -Body $createEventBody -Token $orgToken
    Test-Check "Organiser creates event" 201 $r.StatusCode
    $newEventId = Get-Field $r.Content "id"

    $createTcBody = '{"name":"General","price":500.00,"totalSeats":10}'
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/events/$newEventId/ticket-categories" -Body $createTcBody -Token $orgToken
    Test-Check "Organiser adds ticket category" 201 $r.StatusCode
    $newTcId = Get-Field $r.Content "id"

    Write-Host "-- Attendee: book + view + cancel --"
    $bookBody = "{`"ticketCategoryId`":`"$newTcId`",`"quantity`":2}"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/bookings" -Body $bookBody -Token $attendeeToken
    Test-Check "Attendee books 2 tickets" 201 $r.StatusCode
    $bookingId = Get-Field $r.Content "id"

    $overbookBody = "{`"ticketCategoryId`":`"$newTcId`",`"quantity`":50}"
    $r = Invoke-Api -Method POST -Url "$baseUrl/api/bookings" -Body $overbookBody -Token $attendeeToken
    Test-Check "Booking more than available seats is rejected" 400 $r.StatusCode

    $r = Invoke-Api -Method GET -Url "$baseUrl/api/bookings/mine" -Token $attendeeToken
    Test-Check "Attendee views their bookings" 200 $r.StatusCode

    $r = Invoke-Api -Method PUT -Url "$baseUrl/api/bookings/$bookingId/cancel" -Token $attendeeToken
    Test-Check "Attendee cancels their booking" 200 $r.StatusCode

    $r = Invoke-Api -Method PUT -Url "$baseUrl/api/bookings/$bookingId/cancel" -Token $attendeeToken
    Test-Check "Cancelling an already-cancelled booking is rejected" 400 $r.StatusCode

    Write-Host "-- Authorization checks --"
    $r = Invoke-Api -Method GET -Url "$baseUrl/api/bookings/mine"
    Test-Check "Bookings without a token is rejected" 401 $r.StatusCode

    $r = Invoke-Api -Method POST -Url "$baseUrl/api/admin/categories" -Body '{"name":"Should Fail"}' -Token $attendeeToken
    Test-Check "Attendee cannot create a category (ADMIN only)" 403 $r.StatusCode

    Write-Host "-- Organiser dashboard & Admin --"
    $r = Invoke-Api -Method GET -Url "$baseUrl/api/organiser/events" -Token $orgToken
    Test-Check "Organiser views their dashboard" 200 $r.StatusCode

    $r = Invoke-Api -Method PUT -Url "$baseUrl/api/admin/events/$newEventId/deactivate" -Token $adminToken
    Test-Check "Admin deactivates the smoke-test event" 200 $r.StatusCode

    $r = Invoke-Api -Method PUT -Url "$baseUrl/api/admin/events/$newEventId/activate" -Token $adminToken
    Test-Check "Admin reactivates it" 200 $r.StatusCode

    $r = Invoke-Api -Method DELETE -Url "$baseUrl/api/ticket-categories/$newTcId" -Token $orgToken
    Test-Check "Organiser deletes the ticket category" 204 $r.StatusCode

    $r = Invoke-Api -Method DELETE -Url "$baseUrl/api/events/$newEventId" -Token $orgToken
    Test-Check "Organiser deletes the smoke-test event" 204 $r.StatusCode

    Write-Host ""
    Write-Host "== Result: $script:pass passed, $script:fail failed ==" -ForegroundColor Cyan
    if ($script:fail -ne 0) {
        Write-Host "Backend log at $logFile" -ForegroundColor Yellow
        $script:exitCode = 1
    }
}
finally {
    if ($javaProc -and -not $javaProc.HasExited) {
        Write-Host ""
        Write-Host "Stopping backend (pid $($javaProc.Id))..." -ForegroundColor Cyan
        Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
    }
}

exit $script:exitCode
