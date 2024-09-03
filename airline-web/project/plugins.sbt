// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.22")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

// Resolves similar issue to https://stackoverflow.com/questions/76693403/scala-play-dependency-issue
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")
//
//addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
//
//addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
//
//addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.0")

// eclipse plugins
//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")