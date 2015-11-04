name := """airline-data"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.8.11.2",
  "com.typesafe.akka"          %%  "akka-stream-experimental" % "0.10")
