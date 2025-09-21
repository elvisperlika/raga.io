import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy
import sbtassembly.AssemblyPlugin.autoImport.*

ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case "rootdoc.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val commonSettings = Seq(
  resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % "2.10.9", // For standard log configuration
    "com.typesafe.akka" %% "akka-remote" % "2.10.9", // For akka remote
    "com.typesafe.akka" %% "akka-cluster-typed" % "2.10.9", // Akka clustering module
    "com.typesafe.akka" %% "akka-serialization-jackson" % "2.10.9",
    "com.typesafe.akka" %% "akka-slf4j" % "2.10.9",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.10.9" % Test,
    "ch.qos.logback" % "logback-classic" % "1.5.18",
    "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
  ),
  scalacOptions += "-Wunused:imports"
)

// ---

lazy val protocol = (project in file("protocol"))
  .settings(
    name := "protocol"
  )
  .settings(commonSettings)

lazy val motherServerModule = (project in file("mother"))
  .settings(
    name := "mother-server",
    assembly / mainClass := Some("it.unibo.mother.MotherLauncher")
  )
  .settings(commonSettings)
  .dependsOn(protocol)

lazy val childServerModule = (project in file("child"))
  .settings(
    name := "child-server",
    assembly / mainClass := Some("it.unibo.child.ChildLauncher1")
  )
  .settings(commonSettings)
  .dependsOn(protocol)

lazy val clientModule = (project in file("client"))
  .settings(
    name := "client",
    assembly / mainClass := Some("it.unibo.raga.ClientLauncher1")
  )
  .settings(commonSettings)
  .dependsOn(protocol)

// ---

enablePlugins(AssemblyPlugin)
