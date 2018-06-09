name := """airline-data"""

version := "1.1-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  //"org.xerial" % "sqlite-jdbc" % "3.8.11.2",
  "mysql" % "mysql-connector-java" % "5.1.38",
  "com.mchange" % "c3p0" % "0.9.5",
  "com.typesafe.akka"          %%  "akka-stream-experimental" % "0.10",
  "com.typesafe.play"          %%  "play-json" % "2.4.0")
  
  
  
