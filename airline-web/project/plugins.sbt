// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.5")

addSbtPlugin("com.github.sbt" % "sbt-less" % "2.0.1")

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