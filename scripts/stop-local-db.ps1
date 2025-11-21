param(
    [string]$Distro = "Ubuntu"
)

if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
    Write-Error "WSL is required to run this script."
    exit 1
}

$stopScript = "sudo systemctl stop redis-server && sudo systemctl stop postgresql"
& wsl -d $Distro -- bash -lc $stopScript
