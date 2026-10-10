$ErrorActionPreference = 'Stop'
$infra = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $infra '.env'))) {
    throw 'Missing infra/.env. Nothing was stopped.'
}
docker compose --env-file (Join-Path $infra '.env') -f (Join-Path $infra 'docker-compose.yml') stop
