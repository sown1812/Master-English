# Docker development setup

## Prerequisites
- Windows 10/11 with WSL2 enabled
- Docker Desktop installed and running
  - Enable "Use WSL 2 based engine" in Settings → General
  - Enable integration for your WSL distro

## Start services
1. Copy env file:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Edit `.env` if needed (ports, credentials).
3. Start containers:
   ```powershell
   docker compose up -d
   ```
4. Check containers:
   ```powershell
   docker compose ps
   ```

## pgAdmin
- Open http://localhost:8081
- Login: `admin@example.com` / `admin123`
- Add server:
  - Name: Master Postgres
  - Hostname/address: `host.docker.internal`
  - Port: `5432`
  - Username: `master_dev`
  - Password: `master_dev_pwd`

## Run Ktor server
Set environment variables then run:
```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/master_english"
$env:DB_USER = "master_dev"
$env:DB_PASSWORD = "master_dev_pwd"
./gradlew.bat :server:run
```
Server will migrate DB (Flyway) and start at http://localhost:8080.

## Stop services
```powershell
docker compose down
```

## Logs
```powershell
docker compose logs -f db pgadmin
```
