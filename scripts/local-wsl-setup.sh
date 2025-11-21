#!/usr/bin/env bash
set -euo pipefail

if [[ -f /etc/os-release ]]; then
    . /etc/os-release
else
    echo "Cannot detect distro (missing /etc/os-release)"; exit 1
fi

case "$ID" in
  ubuntu|debian)
    REQUIRED_PACKAGES=("redis-server" "postgresql" "postgresql-contrib")
    MISSING=0
    for pkg in "${REQUIRED_PACKAGES[@]}"; do
        if ! dpkg -s "$pkg" >/dev/null 2>&1; then MISSING=1; break; fi
    done
    if (( MISSING )); then
        sudo DEBIAN_FRONTEND=noninteractive apt-get update
        sudo DEBIAN_FRONTEND=noninteractive apt-get install -y "${REQUIRED_PACKAGES[@]}"
    fi
    sudo systemctl enable --now redis-server
    sudo systemctl enable --now postgresql
    ;;
  arch)
    REQUIRED_PACKAGES=("redis" "postgresql")
    sudo pacman -Sy --noconfirm "${REQUIRED_PACKAGES[@]}"
    # Initialize Postgres data dir if empty (Arch ships uninitialized)
    if [[ ! -d /var/lib/postgres/data/base ]]; then
      sudo -iu postgres initdb -D /var/lib/postgres/data
    fi
    sudo systemctl enable --now redis
    sudo systemctl enable --now postgresql
    ;;
  *)
    echo "Unsupported distro ID=$ID. Please install Redis/PostgreSQL manually."; exit 1
    ;;
esac

if ! sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname = 'shortlink'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE USER shortlink WITH PASSWORD 'change-me';"
fi
if ! sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname = 'shortlink'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE DATABASE shortlink OWNER shortlink;"
fi
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE shortlink TO shortlink;"
