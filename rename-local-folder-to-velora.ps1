# Close Cursor/IDE windows using this repo, then run:
#   powershell -ExecutionPolicy Bypass -File .\rename-local-folder-to-velora.ps1
$ErrorActionPreference = "Stop"
$parent = Split-Path $PSScriptRoot -Parent
$src = Join-Path $parent "BitChord"
$dst = Join-Path $parent "Velora"
if (-not (Test-Path -LiteralPath $src)) {
    if (Test-Path -LiteralPath $dst) {
        Write-Host "Already renamed: $dst"
        exit 0
    }
    throw "Neither BitChord nor Velora found under $parent"
}
if (Test-Path -LiteralPath $dst) { throw "$dst already exists" }
Rename-Item -LiteralPath $src -NewName "Velora"
Write-Host "Renamed to $dst"
Write-Host "Reopen the project from: $dst"
