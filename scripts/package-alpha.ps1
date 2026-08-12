$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$vaultRoot = (Resolve-Path (Join-Path $projectRoot "..\..\..")).Path
[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot "pom.xml")
$version = [string]$pom.project.version
$buildRoot = Join-Path $vaultRoot "04 - Builds und Daten\CatPlugins\CatItems\$version"
$uploadRoot = Join-Path $vaultRoot "04 - Builds und Daten\CatPlugins\CatItems\Upload\Latest-Alpha"

Push-Location $projectRoot
try {
    mvn -B clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven-Build fehlgeschlagen."
    }

    New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
    Copy-Item -LiteralPath "target\CatItems-$version.jar" -Destination $buildRoot -Force
    Copy-Item -LiteralPath "README.md", "START_HERE.md", "CHANGELOG.md" -Destination $buildRoot -Force
    Copy-Item -LiteralPath "docs" -Destination $buildRoot -Recurse -Force

    New-Item -ItemType Directory -Force -Path $uploadRoot | Out-Null
    $expectedUploadParent = [System.IO.Path]::GetFullPath((Join-Path $vaultRoot "04 - Builds und Daten\CatPlugins\CatItems\Upload"))
    $resolvedUploadRoot = [System.IO.Path]::GetFullPath((Resolve-Path $uploadRoot).Path)
    if (-not $resolvedUploadRoot.StartsWith($expectedUploadParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Unerwartetes Upload-Ziel: $resolvedUploadRoot"
    }
    foreach ($artifact in Get-ChildItem -LiteralPath $resolvedUploadRoot -File -Filter "CatItems-*") {
        $match = [regex]::Match($artifact.Name, '^CatItems-(.+)\.(jar|zip)$')
        if ($match.Success) {
            $archiveRoot = Join-Path (Split-Path $resolvedUploadRoot -Parent) ("Archive\" + $match.Groups[1].Value)
            New-Item -ItemType Directory -Force -Path $archiveRoot | Out-Null
            Move-Item -LiteralPath $artifact.FullName -Destination $archiveRoot -Force
        }
    }
    $archiveParent = Join-Path (Split-Path $resolvedUploadRoot -Parent) "Archive"
    if (Test-Path -LiteralPath $archiveParent -PathType Container) {
        $resolvedArchiveParent = [System.IO.Path]::GetFullPath((Resolve-Path $archiveParent).Path)
        $obsoleteArchives = Get-ChildItem -LiteralPath $resolvedArchiveParent -Directory |
            Sort-Object LastWriteTime, Name -Descending |
            Select-Object -Skip 2
        foreach ($archive in $obsoleteArchives) {
            $resolvedArchive = [System.IO.Path]::GetFullPath($archive.FullName)
            if (-not $resolvedArchive.StartsWith(
                    $resolvedArchiveParent + [System.IO.Path]::DirectorySeparatorChar,
                    [System.StringComparison]::OrdinalIgnoreCase
            )) {
                throw "Unsicherer Archivpfad abgelehnt: $resolvedArchive"
            }
            Remove-Item -LiteralPath $resolvedArchive -Recurse -Force
        }
    }
    Copy-Item -LiteralPath "target\CatItems-$version.jar" -Destination $uploadRoot -Force
    Compress-Archive -Path (Join-Path $buildRoot "*") -DestinationPath (Join-Path $uploadRoot "CatItems-$version.zip") -Force
} finally {
    Pop-Location
}

Write-Host "CatItems-Paket erstellt: $buildRoot"
Write-Host "Upload-Paket erstellt: $uploadRoot"
