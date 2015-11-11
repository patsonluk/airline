name := """airline-rest"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "default" %% "airline-data" % "1.0")
