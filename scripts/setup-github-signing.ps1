param(
    [string]$Repository = "Janakchaudhary/Mitra"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Find-Executable {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [string[]]$FallbackPaths = @()
    )

    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -ne $cmd) {
        return $cmd.Source
    }

    foreach ($candidate in $FallbackPaths) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    return $null
}

function ConvertTo-PlainText {
    param([Parameter(Mandatory = $true)][Security.SecureString]$SecureValue)

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$javaHomeKeytool = $null
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaHomeKeytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
}

$keytool = Find-Executable -Name "keytool.exe" -FallbackPaths @(
    $javaHomeKeytool,
    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
    "C:\Program Files\Java\jdk-17\bin\keytool.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\keytool.exe"
)

# Expand the common Adoptium wildcard manually when needed.
if ($null -eq $keytool) {
    $adoptium = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "bin\keytool.exe" } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if ($null -ne $adoptium) {
        $keytool = $adoptium
    }
}

if ($null -eq $keytool) {
    Write-Host "ERROR: keytool.exe was not found." -ForegroundColor Red
    Write-Host "Install Android Studio/JDK 17, or set JAVA_HOME, then run this script again."
    exit 1
}

$gh = Find-Executable -Name "gh.exe"
if ($null -eq $gh) {
    $gh = Find-Executable -Name "gh"
}
if ($null -eq $gh) {
    Write-Host "ERROR: GitHub CLI (gh) is not installed." -ForegroundColor Red
    Write-Host "Install it with: winget install --id GitHub.cli"
    Write-Host "Then reopen Command Prompt and run: gh auth login"
    exit 1
}

Write-Host "Checking GitHub authentication..."
& $gh auth status | Out-Host
if ($LASTEXITCODE -ne 0) {
    Write-Host "Run: gh auth login" -ForegroundColor Yellow
    exit 1
}

Write-Host "Checking repository access: $Repository"
& $gh repo view $Repository --json nameWithOwner --jq '.nameWithOwner' | Out-Host
if ($LASTEXITCODE -ne 0) {
    Write-Host "Cannot access $Repository with the current GitHub account." -ForegroundColor Red
    exit 1
}

$signingDir = Join-Path $repoRoot ".signing"
$keystoreFile = Join-Path $signingDir "mitra-signing.jks"
$alias = "mitra"
New-Item -ItemType Directory -Path $signingDir -Force | Out-Null

if (Test-Path -LiteralPath $keystoreFile) {
    Write-Host "An existing signing key was found:" -ForegroundColor Yellow
    Write-Host "  $keystoreFile"
    Write-Host "It will NOT be replaced. Reusing the same key is required for APK updates."
    $reuse = Read-Host "Reuse this existing signing key? (Y/N)"
    if ($reuse -notmatch '^[Yy]$') {
        Write-Host "Stopped without changing the signing key."
        exit 1
    }
}

$passwordSecure = Read-Host "Enter the Mitra signing password (minimum 6 characters)" -AsSecureString
$password = ConvertTo-PlainText $passwordSecure

try {
    if ($password.Length -lt 6) {
        throw "Password must be at least 6 characters."
    }

    if (-not (Test-Path -LiteralPath $keystoreFile)) {
        Write-Host "Creating persistent Mitra signing key..."
        & $keytool `
            -genkeypair `
            -keystore $keystoreFile `
            -storetype PKCS12 `
            -storepass $password `
            -keypass $password `
            -alias $alias `
            -keyalg RSA `
            -keysize 3072 `
            -validity 10000 `
            -dname "CN=Mitra Personal App, OU=Personal, O=Mitra, C=IN"

        if ($LASTEXITCODE -ne 0) {
            throw "keytool failed to create the signing key."
        }
    }
    else {
        # Validate that the password and alias match the existing key before uploading secrets.
        & $keytool -list -keystore $keystoreFile -storepass $password -alias $alias *> $null
        if ($LASTEXITCODE -ne 0) {
            throw "The password does not unlock the existing .signing\mitra-signing.jks key, or alias '$alias' is missing."
        }
    }

    Write-Host "Encoding keystore and creating GitHub Actions secrets..."
    $bytes = [IO.File]::ReadAllBytes($keystoreFile)
    $base64Value = [Convert]::ToBase64String($bytes)

    & $gh secret set MITRA_KEYSTORE_BASE64 --repo $Repository --body $base64Value
    if ($LASTEXITCODE -ne 0) { throw "Failed to set MITRA_KEYSTORE_BASE64." }

    & $gh secret set MITRA_KEYSTORE_PASSWORD --repo $Repository --body $password
    if ($LASTEXITCODE -ne 0) { throw "Failed to set MITRA_KEYSTORE_PASSWORD." }

    & $gh secret set MITRA_KEY_ALIAS --repo $Repository --body $alias
    if ($LASTEXITCODE -ne 0) { throw "Failed to set MITRA_KEY_ALIAS." }

    & $gh secret set MITRA_KEY_PASSWORD --repo $Repository --body $password
    if ($LASTEXITCODE -ne 0) { throw "Failed to set MITRA_KEY_PASSWORD." }

    Write-Host ""
    Write-Host "Persistent GitHub signing secrets created successfully." -ForegroundColor Green
    Write-Host ""
    Write-Host "Signing certificate:"
    & $keytool -list -v -keystore $keystoreFile -storepass $password -alias $alias |
        Select-String -Pattern "SHA256:" |
        Select-Object -First 1 |
        ForEach-Object { Write-Host $_.Line.Trim() }

    Write-Host ""
    Write-Host "IMPORTANT BACKUP:" -ForegroundColor Yellow
    Write-Host "  $keystoreFile"
    Write-Host "Copy this .jks file to a safe permanent backup location."
    Write-Host "Also keep the signing password. Never commit the .jks file to Git."
    Write-Host ""
    Write-Host "Verify GitHub secrets with:"
    Write-Host "  gh secret list -R $Repository"
    Write-Host ""
    Write-Host "Then rerun GitHub Actions -> Android build."
}
finally {
    $password = $null
    $base64Value = $null
    [GC]::Collect()
}
