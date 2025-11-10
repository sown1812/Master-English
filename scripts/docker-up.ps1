Param(
  [switch]$Recreate
)
if ($Recreate) {
  docker compose down -v
}
docker compose up -d
