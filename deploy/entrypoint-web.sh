#!/bin/sh
set -eu

# pidfile to /dev/null: a stale RUNNING_PID would otherwise block every
# container restart and defeat restart-on-crash.
exec /app/airline-web/bin/airline-web \
  -J-Xmx"${WEB_HEAP:-1024m}" \
  -Dplay.server.pidfile.path=/dev/null \
  -Dhttp.address=0.0.0.0 \
  -Dhttp.port=9000 \
  -Dplay.http.secret.key="${SECRET_KEY:?SECRET_KEY is required}" \
  -Dmysqldb.host="${DB_HOST:-db:3306}" \
  -Dmysqldb.schema="${DB_SCHEMA:-airline_v2_1}" \
  -Dmysqldb.user="${DB_USER:-sa}" \
  -Dmysqldb.password="${DB_PASSWORD:?DB_PASSWORD is required}" \
  -Dsim.pekko-actor.host="${SIM_HOST:-sim}:2552" \
  -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname="${WEB_HOST:-web}" \
  -DwebsocketActorSystem.pekko.remote.artery.canonical.port=10999 \
  -DwebsocketActorSystem.pekko.remote.artery.bind.hostname=0.0.0.0 \
  -DwebsocketActorSystem.pekko.remote.artery.bind.port=10999 \
  -Delasticsearch.host="${ES_HOST:-es}" \
  -Delasticsearch.port="${ES_PORT:-9200}" \
  -Dgoogle.mapKey="${GOOGLE_MAP_KEY:-}" \
  -Dgoogle.apiKey="${GOOGLE_API_KEY:-}" \
  "$@"
