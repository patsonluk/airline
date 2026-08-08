#!/bin/sh
set -eu

# Map env vars to -D system properties. The game reads config through
# ConfigFactory.load() only; it does not read env vars itself.
exec /app/airline-data/bin/airline-data \
  -J-Xms"${SIM_HEAP:-8g}" -J-Xmx"${SIM_HEAP:-8g}" \
  -J-XX:MetaspaceSize=64m -J-XX:MaxMetaspaceSize=256m \
  -Dlog4j2.formatMsgNoLookups=true \
  -Dmysqldb.host="${DB_HOST:-db:3306}" \
  -Dmysqldb.schema="${DB_SCHEMA:-airline_v2_1}" \
  -Dmysqldb.user="${DB_USER:-sa}" \
  -Dmysqldb.password="${DB_PASSWORD:?DB_PASSWORD is required}" \
  -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname="${SIM_HOST:-sim}" \
  -DwebsocketActorSystem.pekko.remote.artery.canonical.port=2552 \
  -Ddev="${DEV_MODE:-false}" \
  "$@"
