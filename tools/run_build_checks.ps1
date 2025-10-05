<#!
.SYNOPSIS
Runs the reproducible build and dependency insight commands used to validate the
JavaPoet/Hilt setup.

.PARAMETER ProjectPath
Optional path to the project root containing gradlew.bat. Defaults to the
directory where this script lives.

.PARAMETER SkipClean
Skips the clean build step when set to $true. Useful when you only need the
incremental build.

.PARAMETER SkipIncremental
Skips the second assembleDebug run. Useful when you only need the clean build.

.NOTES
This script mirrors the investigation runbook so future agents can capture
evidence quickly.
#>

[CmdletBinding()]
param(
    [string]$ProjectPath,
    [switch]$SkipClean,
    [switch]$SkipIncremental
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-Gradle {
    param(
        [string]$ProjectDir,
        [string[]]$Arguments,
        [string]$Description
    )

    Write-Host "`n=== $Description ===" -ForegroundColor Cyan
    Push-Location -LiteralPath $ProjectDir
    try {
        & "$ProjectDir\gradlew.bat" @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "gradlew.bat exited with code $LASTEXITCODE during '$Description'"
        }
    }
    finally {
        Pop-Location
    }
}

if (-not $ProjectPath) {
    if (-not $PSScriptRoot) {
        throw 'Unable to determine project path because -ProjectPath was not provided and $PSScriptRoot is empty.'
    }
    $ProjectPath = (Get-Item -LiteralPath $PSScriptRoot).Parent.FullName
} else {
    $ProjectPath = (Get-Item -LiteralPath $ProjectPath).FullName
}

if (-not (Test-Path -LiteralPath (Join-Path -Path $ProjectPath -ChildPath 'gradlew.bat'))) {
    throw "Could not find gradlew.bat in '$ProjectPath'. Adjust -ProjectPath."
}

$dependencyCommands = @(
    @{ Description = 'Classpath JavaPoet insight'; Args = @('dependencyInsight', '--configuration', 'buildscriptDependenciesMetadata', '--dependency', 'javapoet') },
    @{ Description = 'KSP debug JavaPoet insight'; Args = @(':app:dependencyInsight', '--configuration', 'kspDebugKotlinCompileClasspath', '--dependency', 'javapoet') }
)

Invoke-Gradle -ProjectDir $ProjectPath -Arguments @('--refresh-dependencies', ':app:assembleDebug', '--stacktrace') -Description 'Initial assembleDebug with refreshed dependencies'

foreach ($cmd in $dependencyCommands) {
    try {
        Invoke-Gradle -ProjectDir $ProjectPath -Arguments $cmd.Args -Description $cmd.Description
    }
    catch {
        Write-Warning "$($cmd.Description) failed: $($_.Exception.Message)"
    }
}

if (-not $SkipClean) {
    Invoke-Gradle -ProjectDir $ProjectPath -Arguments @('clean', ':app:assembleDebug') -Description 'Clean assembleDebug'
}

if (-not $SkipIncremental) {
    Invoke-Gradle -ProjectDir $ProjectPath -Arguments @(':app:assembleDebug') -Description 'Incremental assembleDebug'
}

Write-Host "`nAll requested Gradle checks completed." -ForegroundColor Green
