# Containerized Development Environment for Airline Sim

This repository includes a Docker Compose setup that provides a consistent development environment for the Airline Sim project. The compose file defines service images, networking, persistence volumes, and healthchecks so you can run the full application stack locally.

## Prerequisites

- Docker: Install Docker Desktop (or an equivalent runtime). For Windows, ensure WSL2 is enabled and a Linux distribution is available. Rancher Desktop is another option for Windows users.

## Service Definitions

- **db**: `mysql:5.7`
- **airline-init**: one-time initialization of database schema / migrations.
- **airline-sim**: backend simulation service.
- **elasticsearch**: `docker.elastic.co/elasticsearch/elasticsearch:7.17.10`
- **airline-web**: Play web UI for Airline Sim.

## Volumes

- `db_data`: persists MySQL data at `/var/lib/mysql` so DB state survives container recreation.
- `es_data`: persists Elasticsearch data at `/usr/share/elasticsearch/data`.

## How to build and run

1. Build images

```bash
docker compose build
```

2. Run one-time initialization of the database schema and data fill. The `airline-init` service is placed in the `init` profile, so run it with the profile flag. This will start only the services required for init.

> ⚠️ Important - running the init profile is required before starting the full stack, as it sets up the database schema. If you skip this step, the application will not function correctly.

> ⚠️ If you run init again later, it will overwrite existing data in the database.

```bash
docker compose --profile init up
# When init completes, stop/remove the init containers with:
docker compose --profile init down
```

3. Start the full development environment:

```bash
docker compose up -d
# or for foreground logs:
docker compose up
```

4. Stop and remove containers when finished:

```bash
docker compose down
```

## Accessing services

- Web UI: `http://localhost:9000`
- Elasticsearch: `http://localhost:9200` (used by services, can be queried directly)
- MySQL: `localhost:3306` (connect using user `sa` / password `admin`)

## Environment and overrides

- Compose sets environment variables for each service inline in the `docker-compose.yaml`. You can override these by using an `.env` file or by using compose override files (`docker-compose.override.yaml`) if you need different credentials, hosts, or ports.

## Notes & Modifications from upstream

- The repository's `Dockerfile` was updated to use OpenJDK 11 for compatibility with modern builds.
- `airline-web` includes an updated Elasticsearch configuration in `SearchUtil.java` that reads host/port from environment variables (e.g., `ES_HOST`) so it can connect to the bundled `elasticsearch` service.

## Suggestions and Contributions

This is provided to the community for easier local development. Suggestions and contributions to improve the setup are welcome!