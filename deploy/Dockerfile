# syntax=docker/dockerfile:1
# Build context is the repo root. Build with:
#   docker build -f deploy/Dockerfile --target sim -t airline-sim .
#   docker build -f deploy/Dockerfile --target web -t airline-web .

# ---- builder ----
# The image's pre-installed Scala (2.13.13) is irrelevant; sbt downloads the
# project's own Scala 2.13.14. The tag matters for JDK 17 + sbt 1.9.9.
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.9.9_2.13.13 AS builder
WORKDIR /build
COPY airline-data /build/airline-data
COPY airline-web  /build/airline-web
# airline-web depends on airline-data as a published artifact ("default" %% "airline-data" % "2.1"),
# so publishLocal must run before the web build. `stage` copies every dependency jar into
# target/universal/stage/lib, so the runtime stages need nothing from the cache mounts.
RUN --mount=type=cache,target=/root/.ivy2 \
    --mount=type=cache,target=/root/.sbt \
    --mount=type=cache,target=/root/.cache \
    cd /build/airline-data && sbt -mem 2048 publishLocal stage && \
    cd /build/airline-web  && sbt -mem 2048 stage

# ---- sim runtime ----
FROM eclipse-temurin:17-jre-jammy AS sim
WORKDIR /app/airline-data
COPY --from=builder /build/airline-data/target/universal/stage /app/airline-data
# World-data files: the init/patcher mains read them relative to CWD (WORKDIR above).
COPY airline-data/*.csv airline-data/*.txt /app/airline-data/
COPY deploy/entrypoint-sim.sh /app/entrypoint-sim.sh
RUN chmod +x /app/entrypoint-sim.sh
ENTRYPOINT ["/app/entrypoint-sim.sh"]

# ---- web runtime ----
FROM eclipse-temurin:17-jre-jammy AS web
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app/airline-web
COPY --from=builder /build/airline-web/target/universal/stage /app/airline-web
COPY deploy/entrypoint-web.sh /app/entrypoint-web.sh
RUN chmod +x /app/entrypoint-web.sh
EXPOSE 9000
ENTRYPOINT ["/app/entrypoint-web.sh"]
