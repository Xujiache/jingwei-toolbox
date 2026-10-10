$ErrorActionPreference = 'Stop'
$infra = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $infra '.env'))) {
    throw 'Missing infra/.env. Copy infra/.env.example to infra/.env and set development-only values.'
}
docker compose --env-file (Join-Path $infra '.env') -f (Join-Path $infra 'docker-compose.yml') up -d
