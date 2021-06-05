name := """airline-data"""

version := "2.0"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
  //"org.xerial" % "sqlite-jdbc" % "3.8.11.2",
  "mysql" % "mysql-connector-java" % "5.1.49",
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.13.0",
  "com.typesafe.akka" %% "akka-actor" % "2.5.26",
  "com.typesafe.akka"          %%  "akka-stream" % "2.5.26",
  "com.typesafe.akka" %% "akka-remote" % "2.5.26",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.26" % Test,
  "com.typesafe.play"          %%  "play-json" % "2.7.4",
  "com.mchange" % "c3p0" % "0.9.5.5",
  "com.google.guava" % "guava" % "22.0")

  
  
  
