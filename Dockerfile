FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.9.0_2.13.11

WORKDIR /app

# Copy source code
COPY airline-data /app/airline-data
COPY airline-web /app/airline-web
COPY project /app/project

# Install mysql client so entrypoint can apply SQL patches during init
RUN apt-get update && apt-get install -y default-mysql-client

# Publish airline-data locally so airline-web can find it
WORKDIR /app/airline-data
RUN sbt publishLocal

# Pre-compile airline-web
WORKDIR /app/airline-web
RUN sbt compile

WORKDIR /app
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
