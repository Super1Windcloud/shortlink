param(
    [string]$Distro = "Ubuntu"
)

if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
    Write-Error "WSL is required to run this script."
    exit 1
}

$scriptPath = (Resolve-Path scripts/local-wsl-setup.sh).Path
$escaped = $scriptPath.Replace('\','\\')
$linuxPath = & wsl wslpath -u "$escaped"
if (-not $linuxPath) {
    Write-Error "Failed to translate `$scriptPath` into a WSL path."
    exit 1
}

& wsl -d $Distro -- bash "$linuxPath"
