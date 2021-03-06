
import sbt.Project.projectToRef

name := """scalaapp"""

version := "1.0-SNAPSHOT"

// lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"
lazy val clients = Seq(client)
lazy val scalaV = "2.11.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import",     // 2.11 only
  "-Yno-predef",   // no automatic import of Predef (removes irritating implicits)
  "-Yno-imports"  // no automatic imports at all; all symbols must be imported explicitly
)


/* libraryDependencies ++= Seq(
  cache,
  ws,
  evolutions,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.h2database" % "h2" % "1.3.175",
  "mysql" % "mysql-connector-java" % "5.1.12"
) */

// resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    cache,
    ws,
    evolutions,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
    "com.typesafe.play" %% "play-slick" % "2.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
    "com.h2database" % "h2" % "1.3.175",
    "mysql" % "mysql-connector-java" % "5.1.12",
    "com.vmunier" %% "play-scalajs-scripts" % "0.5.0",
    "org.webjars" % "jquery" % "1.11.1"
  )

).enablePlugins(PlayScala).aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "be.doeraene" %%% "scalajs-jquery" % "0.9.0"
  )

//  skip in packageJSDependencies := false,
//  jsDependencies += "org.webjars" % "jquery" % "3.0.0" / "Jquery.js"
//  jsDependencies += "org.webjars" % "jquery" % "3.0.0"

  // libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.0"
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(sharedJs)


lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
// set scalaJSUseRhino in Global := false
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
