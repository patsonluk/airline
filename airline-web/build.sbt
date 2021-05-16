name := """airline-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  guice,
  specs2 % Test,
  "com.typesafe.akka" %% "akka-remote" % "2.5.26",
  "default" %% "airline-data" % "2.0",
  "com.google.api-client" % "google-api-client" % "1.30.4",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.30.4",
  "com.google.apis" % "google-api-services-gmail" % "v1-rev103-1.25.0",
  "javax.mail" % "javax.mail-api" % "1.6.2",
  "com.sun.mail" % "javax.mail" % "1.6.2"
)

// https://mvnrepository.com/artifact/org.elasticsearch.client/elasticsearch-rest-client
libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.6.2"




resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
