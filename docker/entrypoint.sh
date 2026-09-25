#!/usr/bin/env bash
set -euo pipefail

ROLE="${1:-web}"

DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-airline_v2_1}"
DB_USER="${DB_USER:-sa}"
DB_PASSWORD="${DB_PASSWORD:-admin}"
SIM_HOST="${SIM_HOST:-simulation}"   # container name of the simulation service
WEB_HOST="${WEB_HOST:-web}"          # container name of the web service
JAVA_MEM="${JAVA_MEM:--Xms512m -Xmx2g}"

# All of these are read through Typesafe ConfigFactory.load(), where -D system
# properties override application.conf.
DB_OPTS=(
  "-Dmysqldb.host=${DB_HOST}:${DB_PORT}"
  "-Dmysqldb.schema=${DB_NAME}"
  "-Dmysqldb.user=${DB_USER}"
  "-Dmysqldb.password=${DB_PASSWORD}"
  "-Dlog4j2.formatMsgNoLookups=true"
)

wait_for() {
  local host="$1" port="$2"
  echo "Waiting for ${host}:${port} ..."
  until nc -z "$host" "$port"; do sleep 2; done
}

case "$ROLE" in
  init)
    wait_for "$DB_HOST" "$DB_PORT"
    cd /app/data
    exec java $JAVA_MEM "${DB_OPTS[@]}" -cp "/app/data/lib/*" com.patson.init.MainInit
    ;;

  simulation)
    wait_for "$DB_HOST" "$DB_PORT"
    cd /app/data
    # Pekko remoting: the web container connects to us as ${SIM_HOST}:2552,
    # so that must be our canonical (advertised) address.
    exec java $JAVA_MEM "${DB_OPTS[@]}" \
      -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname="${SIM_HOST}" \
      -DwebsocketActorSystem.pekko.remote.artery.canonical.port=2552 \
      -DwebsocketActorSystem.pekko.remote.artery.bind.hostname=0.0.0.0 \
      -DwebsocketActorSystem.pekko.remote.artery.bind.port=2552 \
      -cp "/app/data/lib/*" com.patson.MainSimulation
    ;;

  web)
    wait_for "$DB_HOST" "$DB_PORT"
    : "${APPLICATION_SECRET:?APPLICATION_SECRET must be set (32+ random chars)}"
    exec /app/web/bin/airline-web \
      -J-Xms512m -J-Xmx1g \
      "${DB_OPTS[@]}" \
      -Dhttp.port=9000 \
      -Dpidfile.path=/dev/null \
      -Dplay.http.secret.key="${APPLICATION_SECRET}" \
      -Dgoogle.mapKey="${GOOGLE_MAP_KEY:-your key}" \
      -Dgoogle.apiKey="${GOOGLE_API_KEY:-your key}" \
      -Dsim.pekko-actor.host="${SIM_HOST}:2552" \
      -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname="${WEB_HOST}" \
      -DwebsocketActorSystem.pekko.remote.artery.canonical.port=10999 \
      -DwebsocketActorSystem.pekko.remote.artery.bind.hostname=0.0.0.0 \
      -DwebsocketActorSystem.pekko.remote.artery.bind.port=10999
    ;;

  search-index)
    wait_for "$DB_HOST" "$DB_PORT"
    exec java $JAVA_MEM "${DB_OPTS[@]}" -cp "/app/web/lib/*" controllers.SearchUtil
    ;;

  *)
    exec "$@"
    ;;
esac
