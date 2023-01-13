version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayService, PlayLayoutPlugin)
  .settings(
    name := "RMTBackend"
  )

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

libraryDependencies ++= Seq(
  guice,
  "org.postgresql" % "postgresql" % "42.5.0",
  "com.typesafe" % "config" % "1.4.2",
  // pre-configured logback
  "joda-time" % "joda-time" % "2.12.1",
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.6.0",
  "commons-validator" % "commons-validator" % "1.7",
  "commons-codec" % "commons-codec" % "1.15",
  "com.github.jwt-scala" %% "jwt-play" % "9.1.1",
  "org.typelevel" %% "cats-core" % "2.8.0"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
