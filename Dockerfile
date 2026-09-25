# One image, four roles (see docker/entrypoint.sh):
#   init        -> one-off DB schema + data load (MainInit)
#   simulation  -> background simulation (MainSimulation)
#   web         -> Play front-end on :9000
#   search-index-> optional: fill Elasticsearch for "Flight search"

############################
# Build stage
############################
FROM eclipse-temurin:17-jdk AS build

ARG SBT_VERSION=1.9.9
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" | tar xz -C /opt
ENV PATH="/opt/sbt/bin:${PATH}" \
    SBT_OPTS="-Xmx2g -Xss4m"

WORKDIR /src
COPY airline-data airline-data
COPY airline-web  airline-web

# 1) airline-data has no packaging plugin; add sbt-native-packager at build time
#    so `stage` gives us a clean lib/ folder with every runtime jar.
# 2) Elasticsearch host is hard-coded to localhost:9200; make it configurable
#    via the ELASTICSEARCH_URL env var.
RUN printf '\naddSbtPlugin("com.github.sbt" %% "sbt-native-packager" %% "1.9.16")\n' >> airline-data/project/plugins.sbt \
 && printf '\nenablePlugins(JavaAppPackaging)\nCompile / mainClass := Some("com.patson.MainSimulation")\n' >> airline-data/build.sbt \
 && perl -0pi -e 's/new HttpHost\("localhost", 9200, "http"\),\s*new HttpHost\("localhost", 9201, "http"\)/HttpHost.create(System.getenv().getOrDefault("ELASTICSEARCH_URL", "http:\/\/localhost:9200"))/' \
      airline-web/app/controllers/SearchUtil.java

# airline-web depends on airline-data via publishLocal, so build data first.
RUN --mount=type=cache,target=/root/.cache/coursier \
    --mount=type=cache,target=/root/.sbt \
    cd airline-data && sbt -batch publishLocal stage \
 && cd ../airline-web && sbt -batch stage

############################
# Runtime stage
############################
FROM eclipse-temurin:17-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat-openbsd \
 && rm -rf /var/lib/apt/lists/*

# Simulation/init code reads its CSV/TXT inputs from the working directory,
# so ship the airline-data folder (minus sources) as /app/data.
WORKDIR /app/data
COPY airline-data/ /app/data/
RUN rm -rf /app/data/src /app/data/project /app/data/target

COPY --from=build /src/airline-data/target/universal/stage/lib /app/data/lib
COPY --from=build /src/airline-web/target/universal/stage      /app/web
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN sed -i "s/\r$//" /usr/local/bin/entrypoint.sh \
 && chmod +x /usr/local/bin/entrypoint.sh /app/web/bin/airline-web

EXPOSE 9000 2552 10999
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["web"]
