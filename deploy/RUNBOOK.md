# Airline Club — Docker deployment runbook

## Context

*(This section is normal prose; the procedures below are STE.)*

This runbook moves the game from a bare-metal install to Docker Compose on the same 16 GB GCP VM,
and stands up a password-gated ops panel so admins can restart the game without SSH. Docker's only
automatic behavior is restart-on-crash: a container whose process dies comes back by itself; a
container stopped on purpose stays down. Nothing runs `MainInit` or patchers automatically — those
remain deliberate command-line actions because `MainInit` drops every table.

The stack: `db` (MySQL 5.7), `es` (Elasticsearch 7.17), `sim` (backend simulation), `web` (Play
front end, port 9000), `panel` (ops panel, port 8080), and `socket-proxy` (limits the panel's Docker
API access to container status/logs/start/stop/restart). The old bare-metal install stays intact
until you decide to remove it, so rollback is always available.

## 1. Install Docker on the VM

1. Install Docker Engine and the compose plugin:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   ```
2. Add your user to the docker group:
   ```bash
   sudo usermod -aG docker $USER
   ```
3. Log out and log in again.
4. Make sure that Docker starts at boot:
   ```bash
   sudo systemctl enable docker
   ```
5. Check the installation:
   ```bash
   docker compose version
   ```

## 2. Get the code and set the configuration

1. Clone the repository and check out the branch `deploy/docker`.
2. Copy the environment template:
   ```bash
   cp deploy/.env.example deploy/.env
   ```
3. Open `deploy/.env` and set each value. The file has instructions for each key.
4. Generate `SECRET_KEY` and `PANEL_SECRET_KEY`:
   ```bash
   openssl rand -hex 32
   ```
5. Generate `PANEL_PASSWORD_HASH` with the command shown in `deploy/.env.example`.

Heap budget (16 GB VM): the web needs approximately 1 GB. The sim and Elasticsearch use the
remainder. Keep 1 to 2 GB free for MySQL, the panel, and the OS. Swap use under load is expected.
To change a heap size later: edit `SIM_HEAP`, `WEB_HEAP`, or `ES_HEAP` in `deploy/.env`, then run
`docker compose -f deploy/docker-compose.yml up -d`. No image rebuild is necessary.

## 3. Build the images

1. Run:
   ```bash
   docker compose -f deploy/docker-compose.yml build
   ```

The first build downloads all sbt dependencies and can take 20 to 40 minutes. Later builds use the
build cache and are much faster. If the VM is under memory pressure from the live game, build on a
different machine and move the images with `docker save` and `docker load`.

## 4. Import the production database

Do the steps in this section for a migration of an existing install.

1. On the old install, export the database:
   ```bash
   mysqldump --single-transaction --default-character-set=utf8mb4 -u sa -p airline_v2_1 > dump.sql
   ```
2. Start only the database container:
   ```bash
   docker compose -f deploy/docker-compose.yml up -d db
   ```
3. Wait until `docker ps` shows the db container as `healthy`.
4. Import the dump:
   ```bash
   docker compose -f deploy/docker-compose.yml exec -T db sh -c 'mysql -usa -p"$MYSQL_PASSWORD" airline_v2_1' < dump.sql
   ```

## 5. Initialize a fresh database (dev only)

**WARNING: `MainInit` drops all tables and deletes all game data. Do not run it against a
database with player data. This procedure is for a fresh, empty install only. It is never
automated; the dev runs it by hand.**

1. Run the init main:
   ```bash
   docker compose -f deploy/docker-compose.yml run --rm sim -main com.patson.init.MainInit
   ```
2. After it completes, run the mandatory patcher:
   ```bash
   docker compose -f deploy/docker-compose.yml run --rm sim -main com.patson.patch.Version2_1Patcher
   ```

The patcher creates the tables `alliance_stats`, `alliance_mission`, and `airport_asset`. Without
them, the alliance simulation fails on each cycle.

## 6. First launch

1. Start the full stack:
   ```bash
   docker compose -f deploy/docker-compose.yml up -d
   ```
2. Watch the sim log until it shows that the simulation runs:
   ```bash
   docker compose -f deploy/docker-compose.yml logs -f sim
   ```
3. Check the web front end:
   ```bash
   curl -fs http://localhost:9000/ > /dev/null && echo WEB_OK
   ```

## 7. Load the Elasticsearch index

Do this once after the first launch, and again after a database import.

**CAUTION: If Elasticsearch runs with an empty index, the alliance page returns errors on each
cycle. Either load the index or stop the `es` container.**

1. Run the index loader (approximately 4 minutes):
   ```bash
   docker compose -f deploy/docker-compose.yml run --rm web -main controllers.SearchUtil
   ```
2. Check the index:
   ```bash
   docker compose -f deploy/docker-compose.yml exec es curl -s 'localhost:9200/_cat/indices?v'
   ```
   Expected document counts: airports ≈ 3800, countries ≈ 233, zones 6, plus airlines and alliances.

## 8. Ops panel

1. Open `http://<vm-ip>:8080` in a browser.
2. Log in with the admin password.
3. The panel shows each container with its state and health.
4. Use Restart, Stop, and Start on `sim`, `web`, and `es`.
5. Use Logs to read the last 200 log lines of any container, `db` included.

The panel cannot stop or restart `db`. A database problem is an escalation to the dev.

Recommended hardening (optional, no TLS required): add a GCP firewall rule that limits TCP port
8080 to admin IP addresses. Use a long random panel password.

## 9. Cutover checklist

1. Tell the players about the downtime.
2. Stop the old sim process (the `MainSimulation` java process).
3. Stop the old web process.
4. Export the database (section 4, step 1).
5. Import the database (section 4, steps 2 to 4).
6. Start the stack (section 6).
7. Load the Elasticsearch index (section 7).
8. Smoke test: log in as a player, open the map, search for an airport.
9. Watch the sim log until one full cycle completes.
10. Point the firewall or DNS at port 9000.

## 10. Rollback

1. Stop the stack:
   ```bash
   docker compose -f deploy/docker-compose.yml down
   ```
2. Start the old bare-metal install (`airline-data/start.sh` and the old web start procedure).
3. If players wrote data while Docker was live, export from the db container first and import
   into the bare-metal MySQL.

## 11. Operations reference

- Read all logs: `docker compose -f deploy/docker-compose.yml logs -f <service>`.
- After a VM reboot, the stack starts by itself (restart policy + Docker at boot).
- A crashed container restarts by itself. A wedged-but-alive process does not; restart it from
  the panel.
- If live updates on the site stay stale after a sim restart, restart `web` from the panel.
- Disk cleanup: `docker system prune` removes old images and build cache. It does not touch the
  `dbdata` and `esdata` volumes.
- **WARNING: `docker compose down -v` deletes the database volume. Do not use the `-v` flag.**

## Known limits

*(Normal prose.)*

- Email password-reset and the Google Photos banner feature depend on token files
  (`airline-web/tokens`, `google-tokens`) that are not baked into the images. If production uses
  them, add a bind mount for those directories — confirm with the dev at cutover.
- The panel has no TLS by design; the password travels in clear text on the network path. The GCP
  firewall rule above is the compensating control.
- MySQL 5.7 images are amd64-only; on an Apple-silicon dev machine the db container runs emulated.
