lazy val airlineData = project.in(file("airline-data"))

lazy val airlineWeb = project.in(file("airline-web")).enablePlugins(PlayScala)

lazy val root = project.in(file("."))
  .aggregate(airlineData, airlineWeb)
