#Requires -Version 5.1
<#
.SYNOPSIS
  Gera dozamigos-manifest.json (mods + files do pack) e mostra o diff.

.EXAMPLE
  .\generate-manifest.ps1 `
    -GameDir "..\modpack" `
    -PackVersion "1.0.0" `
    -ReleaseBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack/mods" `
    -FilesUrlBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack" `
    -OutFile "..\dozamigos-manifest.json"
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $GameDir,

    [Parameter(Mandatory = $true)]
    [string] $PackVersion,

    [Parameter(Mandatory = $true)]
    [string] $ReleaseBase,

    [Parameter(Mandatory = $true)]
    [string] $FilesUrlBase,

    [string] $OutFile = "",
    [string] $PreviousFile = "",
    [string] $PreviousUrl = "",
    [string] $ExcludeFile = "",
    [string] $IncludeFile = "",
    [string] $IdOverridesFile = "",
    [switch] $DryRun
)

$ErrorActionPreference = "Stop"

if (-not $OutFile) {
    $OutFile = Join-Path (Split-Path $PSScriptRoot -Parent) "dozamigos-manifest.json"
}
if (-not $ExcludeFile) { $ExcludeFile = Join-Path $PSScriptRoot "manifest-exclude.txt" }
if (-not $IncludeFile) { $IncludeFile = Join-Path $PSScriptRoot "manifest-include.txt" }
if (-not $IdOverridesFile) { $IdOverridesFile = Join-Path $PSScriptRoot "manifest-id-overrides.txt" }

function Write-Tag([string]$tag, [ConsoleColor]$color, [string]$msg) {
    Write-Host ("[{0}] " -f $tag) -ForegroundColor $color -NoNewline
    Write-Host $msg
}

function Read-Lines([string]$path) {
    if (-not (Test-Path $path)) { return @() }
    @(Get-Content $path | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") })
}

function Read-IdOverrides([string]$path) {
    $map = @{}
    if (-not (Test-Path $path)) { return $map }
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) { $map[$parts[0].Trim()] = $parts[1].Trim() }
    }
    return $map
}

function Test-Excluded([string]$name, [string[]]$patterns) {
    foreach ($p in $patterns) {
        if ($name -like $p) { return $true }
    }
    return $false
}

# Encode each path segment so spaces / unicode keep working on raw.githubusercontent.com
function Get-UrlPath([string]$relativePath) {
    ($relativePath.Replace('\', '/') -split '/' | ForEach-Object {
        [Uri]::EscapeDataString($_)
    }) -join '/'
}

function Get-ModId([string]$fileName, [hashtable]$overrides) {
    $base = [IO.Path]::GetFileNameWithoutExtension($fileName)
    if ($overrides.ContainsKey($base)) { return $overrides[$base] }
    if ($overrides.ContainsKey($fileName)) { return $overrides[$fileName] }

    $id = $base.ToLowerInvariant() -replace ' ', '-'
    $id = $id -replace '[\[\]]', ''
    $id = [regex]::Replace($id, '[_+-]?(forge|neoforge|fabric|quilt|neo)[_+-]?', '-')
    for ($n = 0; $n -lt 8; $n++) {
        $before = $id
        $id = [regex]::Replace($id, '[_+-]v?\d+(\.\d+)*([a-z])?$', '')
        $id = [regex]::Replace($id, '[_+-]mc\d+(\.\d+)*$', '')
        $id = [regex]::Replace($id, '[_+-](build|rc|beta|alpha)\.?\d*$', '')
        $id = [regex]::Replace($id, '[_+-](all|release|lex)$', '')
        if ($id -eq $before) { break }
    }
    if ($null -eq $id) { $id = '' }
    $id = $id.Trim('_', '-', '+', '.', ' ')
    if ([string]::IsNullOrWhiteSpace($id)) { $id = $base.ToLowerInvariant() }
    return $id
}

function Get-FileId([string]$relativePath, [hashtable]$overrides) {
    $key = $relativePath.Replace('\', '/')
    if ($overrides.ContainsKey($key)) { return $overrides[$key] }
    $leaf = [IO.Path]::GetFileNameWithoutExtension($key)
    if ($overrides.ContainsKey($leaf)) { return $overrides[$leaf] }
    return ($key.ToLowerInvariant() -replace '[\\/]', '-' -replace '\.', '-')
}

function Get-PreviousManifest {
    if ($PreviousFile) {
        if (-not (Test-Path $PreviousFile)) { throw "PreviousFile nao encontrado: $PreviousFile" }
        return (Get-Content -Raw $PreviousFile | ConvertFrom-Json)
    }
    if ($PreviousUrl) {
        Write-Host "Baixando manifest anterior: $PreviousUrl"
        return ((Invoke-WebRequest -Uri $PreviousUrl -UseBasicParsing).Content | ConvertFrom-Json)
    }
    if (Test-Path $OutFile) {
        Write-Host "Usando manifest local como anterior: $OutFile"
        return (Get-Content -Raw $OutFile | ConvertFrom-Json)
    }
    return $null
}

function Compare-Section($label, $entries, $prevItems, $pathProp) {
    $prevById = @{}
    foreach ($m in @($prevItems)) {
        if ($null -ne $m -and $m.id) { $prevById[("" + $m.id)] = $m }
    }
    $added = 0; $updated = 0; $removed = 0; $same = 0
    $currIds = @{}

    Write-Host ""
    Write-Host ("--- Diff {0} ---" -f $label) -ForegroundColor White
    if (-not $previous) { Write-Host "  (sem anterior)" -ForegroundColor DarkYellow }

    foreach ($e in $entries) {
        $currIds[$e.id] = $true
        $path = $e.$pathProp
        if (-not $prevById.ContainsKey($e.id)) {
            Write-Tag "+" Green ("{0}  {1}" -f $e.id, $path)
            $added++
            continue
        }
        $old = $prevById[$e.id]
        $oldPath = $old.$pathProp
        $oldHash = ("" + $old.sha256).ToLowerInvariant()
        if (($oldPath -ne $path) -or ($oldHash -ne $e.sha256)) {
            if ($oldPath -eq $path) {
                Write-Tag "~" Yellow ("{0}  {1}  (hash mudou)" -f $e.id, $path)
            } else {
                Write-Tag "~" Yellow ("{0}  {1} -> {2}" -f $e.id, $oldPath, $path)
            }
            $updated++
        } else {
            $same++
        }
    }

    foreach ($id in $prevById.Keys) {
        if (-not $currIds.ContainsKey($id)) {
            $old = $prevById[$id]
            Write-Tag "-" Red ("{0}  {1}" -f $id, $old.$pathProp)
            $removed++
        }
    }

    Write-Host ("Resumo {0}: +{1}  ~{2}  -{3}  ={4}" -f $label, $added, $updated, $removed, $same) -ForegroundColor Cyan
}

if (-not (Test-Path $GameDir)) { throw "GameDir nao existe: $GameDir" }

$ModsDir = Join-Path $GameDir "mods"
if (-not (Test-Path $ModsDir)) { throw "Pasta mods nao encontrada em GameDir" }

$patterns = @(Read-Lines $ExcludeFile)
$includes = @(Read-Lines $IncludeFile)
$overrides = Read-IdOverrides $IdOverridesFile
$releaseBase = $ReleaseBase.TrimEnd("/")
$filesUrlBase = $FilesUrlBase.TrimEnd("/")
$gameRoot = (Resolve-Path $GameDir).Path

$jars = @(Get-ChildItem -Path $ModsDir -Filter "*.jar" -File | Sort-Object Name)
$excluded = New-Object System.Collections.Generic.List[string]
$modEntries = New-Object System.Collections.Generic.List[object]
$fileEntries = New-Object System.Collections.Generic.List[object]
$modIds = @{}; $fileIds = @{}

Write-Host ""
Write-Host "=== Dozamigos manifest generator ===" -ForegroundColor Cyan
Write-Host ("game: {0}" -f $gameRoot)
Write-Host ("mods jars: {0}" -f $jars.Count)
Write-Host ("includes: {0}" -f ($includes -join ", "))
Write-Host ("packVersion: {0}" -f $PackVersion)
Write-Host ""

$i = 0
foreach ($jar in $jars) {
    $i++
    if (Test-Excluded $jar.Name $patterns) {
        $excluded.Add("mod:" + $jar.Name) | Out-Null
        continue
    }
    $id = Get-ModId $jar.Name $overrides
    if ($modIds.ContainsKey($id)) {
        throw ("ID de mod duplicado '{0}' entre '{1}' e '{2}'." -f $id, $modIds[$id], $jar.Name)
    }
    $modIds[$id] = $jar.Name
    Write-Progress -Activity "Hashing mods" -Status $jar.Name -PercentComplete (($i / [math]::Max($jars.Count, 1)) * 100)
    $hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $modEntries.Add([pscustomobject]@{
        id = $id; file = $jar.Name; sha256 = $hash; url = ($releaseBase + "/" + (Get-UrlPath $jar.Name))
    }) | Out-Null
}
Write-Progress -Activity "Hashing mods" -Completed

$packFiles = New-Object System.Collections.Generic.List[System.IO.FileInfo]
foreach ($inc in $includes) {
    $target = Join-Path $gameRoot $inc
    if (-not (Test-Path $target)) {
        Write-Host ("WARN: include ausente: {0}" -f $inc) -ForegroundColor DarkYellow
        continue
    }
    $item = Get-Item -LiteralPath $target
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $item.FullName -Recurse -File | ForEach-Object { $packFiles.Add($_) | Out-Null }
    } else {
        $packFiles.Add($item) | Out-Null
    }
}

$j = 0
foreach ($f in ($packFiles | Sort-Object FullName)) {
    $j++
    if (-not $f.FullName.StartsWith($gameRoot, [StringComparison]::OrdinalIgnoreCase)) { continue }
    $rel = $f.FullName.Substring($gameRoot.Length).TrimStart('\', '/').Replace('\', '/')

    # mods/ ja e secao propria
    if ($rel -like "mods/*") { continue }

    if ((Test-Excluded $rel $patterns) -or (Test-Excluded $f.Name $patterns)) {
        $excluded.Add("file:" + $rel) | Out-Null
        continue
    }

    $id = Get-FileId $rel $overrides
    if ($fileIds.ContainsKey($id)) {
        throw ("ID de file duplicado '{0}' entre '{1}' e '{2}'." -f $id, $fileIds[$id], $rel)
    }
    $fileIds[$id] = $rel

    Write-Progress -Activity "Hashing pack files" -Status $rel -PercentComplete (($j / [math]::Max($packFiles.Count, 1)) * 100)
    $hash = (Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $fileEntries.Add([pscustomobject]@{
        id = $id; path = $rel; sha256 = $hash; url = ($filesUrlBase + "/" + (Get-UrlPath $rel))
    }) | Out-Null
}
Write-Progress -Activity "Hashing pack files" -Completed

$previous = Get-PreviousManifest
$prevFiles = @()
if ($previous) {
    if ($previous.files) { $prevFiles += @($previous.files) }
    if ($previous.configs) { $prevFiles += @($previous.configs) }
}

Write-Host "--- Excluidos ---" -ForegroundColor DarkGray
if ($excluded.Count -eq 0) { Write-Host "  (nenhum)" }
else { foreach ($name in $excluded) { Write-Tag "SKIP" DarkGray $name } }

Compare-Section "mods" $modEntries $(if ($previous) { $previous.mods } else { @() }) "file"
Compare-Section "files" $fileEntries $prevFiles "path"

$manifest = [ordered]@{
    packVersion = $PackVersion
    mods        = @($modEntries | Sort-Object id)
    files       = @($fileEntries | Sort-Object id)
}
$json = $manifest | ConvertTo-Json -Depth 6

if ($DryRun) {
    Write-Host ""
    Write-Host ("DryRun: {0} mods + {1} files (nada escrito)." -f $modEntries.Count, $fileEntries.Count) -ForegroundColor DarkYellow
    exit 0
}

$fullOut = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutFile)
$dir = Split-Path $fullOut -Parent
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
[IO.File]::WriteAllText($fullOut, $json + "`n")

Write-Host ""
Write-Host ("Escrito: {0}" -f $fullOut) -ForegroundColor Green
Write-Host ("mods={0} files={1}" -f $modEntries.Count, $fileEntries.Count)
Write-Host "URLs apontam para o repo (modpack/mods + arquivos em modpack/). Commit o JSON + a pasta modpack."
