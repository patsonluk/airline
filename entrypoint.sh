#!/bin/bash
set -e

# Default values
DB_HOST=${DB_HOST:-"db:3306"}
DB_USER=${DB_USER:-"sa"}
DB_PASSWORD=${DB_PASSWORD:-"admin"}
SIM_HOST=${SIM_HOST:-"airline-sim"}
WEB_HOST=${WEB_HOST:-"0.0.0.0"}

# Common Java Options for DB connection and Memory
# Common Java Options for DB connection and Memory
export JAVA_OPTS="-Xmx8G -Dmysqldb.host=$DB_HOST -Dmysqldb.user=$DB_USER -Dmysqldb.password=$DB_PASSWORD"

case "$1" in
  "init")
    echo "**************************************************************"
    echo "Setting Environment Variables for Initialization"
    export JAVA_OPTS="-Xmx8G -Dmysqldb.host=$DB_HOST -Dmysqldb.user=$DB_USER -Dmysqldb.password=$DB_PASSWORD"
    echo "**************************************************************"
    echo "Initializing Database with Pre-2.1 Patches..."
    cd /app/airline-data
    sbt "runMain com.patson.init.MainInit"
    echo "**************************************************************"
    echo "Install mysql client and applying patches..."
    apt-get update && apt-get install -y default-mysql-client
    echo "**************************************************************"
    echo "Running SQL patches for v2.1"
    PATCH_DIR="/app/airline-data/db_scripts/v2.1"
    PATCH_FILES=("patch_airport.sql" "patch_income_table.sql" "patch_asset_property_history.sql")
    DB_HOST_ONLY=${DB_HOST%%:*}
    DB_PORT_ONLY=${DB_HOST##*:}

    echo "Applying DB patch SQL files from $PATCH_DIR..."
    for f in "${PATCH_FILES[@]}"; do
      if [ -f "$PATCH_DIR/$f" ]; then
        echo "Applying $f"
        mysql -h "$DB_HOST_ONLY" -P "$DB_PORT_ONLY" -u "$DB_USER" -p"$DB_PASSWORD" < "$PATCH_DIR/$f"
      else
        echo "Warning: patch file $PATCH_DIR/$f not found, skipping"
      fi
    done

    echo "**************************************************************"
    echo "Running v2.1 patcher to create any additional tables..."
    cd /app/airline-data
    sbt "runMain com.patson.patch.Version2_1Patcher"

    echo "**************************************************************"
    echo "Running v2.2 SQL patches..."
    PATCH_DIR="/app/airline-data/db_scripts/v2.2"
    PATCH_FILES=("patch_airport_champion_bonus.sql")
    echo "Applying DB patch SQL files from $PATCH_DIR..."
    for f in "${PATCH_FILES[@]}"; do
      if [ -f "$PATCH_DIR/$f" ]; then
        echo "Applying $f"
        mysql -h "$DB_HOST_ONLY" -P "$DB_PORT_ONLY" -u "$DB_USER" -p"$DB_PASSWORD" < "$PATCH_DIR/$f"
      else
        echo "Warning: patch file $PATCH_DIR/$f not found, skipping"
      fi
    done
    ;;
  "sim")
    echo "Starting Simulation..."
    # Configure Pekko Remoting for Simulation
    # We need to bind to 0.0.0.0 but advertise the container hostname/IP
    export JAVA_OPTS="$JAVA_OPTS -Dmysqldb.host=$DB_HOST -Dmysqldb.user=$DB_USER -Dmysqldb.password=$DB_PASSWORD"
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname=$SIM_HOST"
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.bind.hostname=0.0.0.0"
    
    cd /app/airline-data
    sbt "runMain com.patson.MainSimulation"
    ;;
  "web")
    echo "Starting Web Server..."
    # Configure Web to connect to Simulation
    export JAVA_OPTS="$JAVA_OPTS -Dsim.pekko-actor.host=$SIM_HOST:2552"
    
    # Configure Pekko Remoting for Web (so Sim can talk back)
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.canonical.hostname=airline-web"
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.canonical.port=10999"
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.bind.hostname=0.0.0.0"
    export JAVA_OPTS="$JAVA_OPTS -DwebsocketActorSystem.pekko.remote.artery.bind.port=10999"

    # Allow global application for Play
    export JAVA_OPTS="$JAVA_OPTS -Dplay.allowGlobalApplication=true"
    # Bind http port
    export JAVA_OPTS="$JAVA_OPTS -Dhttp.port=9000 -Dhttp.address=0.0.0.0"
    
    cd /app/airline-web
    sbt "run"
    ;;
  *)
    echo "Usage: $0 {init|sim|web}"
    exit 1
    ;;
esac
