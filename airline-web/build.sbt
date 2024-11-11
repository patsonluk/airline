name := """airline-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  guice,
  "com.google.inject" % "guice" % "5.1.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
  specs2 % Test,
  "org.apache.pekko" %% "pekko-remote" % "1.0.3",
  "default" %% "airline-data" % "2.1",
  "com.google.api-client" % "google-api-client" % "1.30.4",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.34.1",
  "com.google.apis" % "google-api-services-gmail" % "v1-rev103-1.25.0",
  "com.google.photos.library" % "google-photos-library-client" % "1.7.2",
  "javax.mail" % "javax.mail-api" % "1.6.2",
  "com.sun.mail" % "javax.mail" % "1.6.2"
)

// https://mvnrepository.com/artifact/org.elasticsearch.client/elasticsearch-rest-client
libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.2"




resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
