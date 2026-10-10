# Running airline in Docker

Everything runs from the repo root with Docker Compose: MySQL, the background
simulation, the Play web server and (optionally) Elasticsearch for flight search.

## Files

| File | Purpose |
|---|---|
| `Dockerfile` | Builds one image with sbt (airline-data → publishLocal → airline-web stage) |
| `docker/entrypoint.sh` | Picks the role: `init`, `simulation`, `web`, `search-index` |
| `docker-compose.yml` | db, simulation, web, plus `init` and `search` profiles |
| `.env.example` | Copy to `.env` and fill in |
| `.dockerignore`, `.gitignore`, `.gitattributes` | Keep builds small, keep `.env` out of git, keep `.sh` files LF on Windows |

## First run

```bash
cp .env.example .env                                   # set APPLICATION_SECRET (openssl rand -hex 32)
docker compose build                                   # first build downloads all sbt deps, takes a while
docker compose up -d db
docker compose run --rm init                           # MainInit: schema + airports/cities/planes (slow, ONCE only)
docker compose --profile search run --rm search-index  # optional: build flight-search indexes
docker compose --profile search up -d                  # start everything (drop --profile search to skip search)
```

Open http://localhost:9000

## Everyday use

```bash
docker compose --profile search up -d     # start (or set COMPOSE_PROFILES=search in .env and just `docker compose up -d`)
docker compose down                       # stop, keeps data
docker compose ps
docker compose logs -f web simulation
```

`up` with the search profile also re-runs `search-index` once, refreshing the indexes.

After pulling code changes: `docker compose build && docker compose up -d`.

**Don't** re-run `init` on an existing database. `docker compose down -v` wipes the
database volume; only then is `init` needed again.

## How it maps to the README

| README step | Docker equivalent |
|---|---|
| MySQL 5.x, db `airline_v2_1`, user `sa`/`admin` | `db` service (mysql:5.7, utf8mb4 set) |
| `publishLocal` airline-data | done in the image build |
| `activator run` → MainInit | `docker compose run --rm init` |
| `activator run` → MainSimulation | `simulation` service |
| `activator run` in airline-web | `web` service (Play prod build via `sbt stage`) |
| Elasticsearch 7.x | `elasticsearch` service (`search` profile) |
| `google.mapKey` in application.conf | `GOOGLE_MAP_KEY` in `.env` |