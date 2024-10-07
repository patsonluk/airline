name := """airline-data"""

version := "2.1"

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
  //"org.xerial" % "sqlite-jdbc" % "3.8.11.2",
  "mysql" % "mysql-connector-java" % "5.1.49",
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.13.0",
  "org.apache.pekko" %% "pekko-actor" % "1.0.3",
  "org.apache.pekko" %% "pekko-stream" % "1.0.3",
  "org.apache.pekko" %% "pekko-remote" % "1.0.3",
  "org.apache.pekko" %% "pekko-testkit" % "1.0.3",
  "org.apache.pekko" %% "pekko-cluster" % "1.0.3",
  "com.typesafe.play"          %%  "play-json" % "2.7.4",
  "com.mchange" % "c3p0" % "0.9.5.5",
  "com.google.guava" % "guava" % "22.0")

  
  
  
