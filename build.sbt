import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy
import sbtassembly.AssemblyPlugin.autoImport.*

ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalaVersion := "3.3.6"

Global / parallelExecution := true

// === AKKA REPOSITORIES (USA IL TUO TOKEN QUI) ===
val AkkaToken = sys.env.get("AKKA_TOKEN") match {
  case Some(token) => token
  case None =>
    sys.error(
      "AKKA_TOKEN environment variable is not set. Please set it to access the Akka secure repository."
    )
}

ThisBuild / resolvers ++= Seq(
  "akka-secure-mvn" at s"https://repo.akka.io/$AkkaToken/secure",
  Resolver.url("akka-secure-ivy", url(s"https://repo.akka.io/$AkkaToken/secure"))(
    Resolver.ivyStylePatterns
  )
)

// === AKKA VERSION ===
val AkkaVersion = "2.10.11"

// === MERGE STRATEGY ===
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case "rootdoc.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

// === COMMON SETTINGS ===
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "ch.qos.logback" % "logback-classic" % "1.5.18",
    "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
  ),
  scalacOptions += "-Wunused:imports"
)

// === MODULES ===
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

// enable assembly
enablePlugins(AssemblyPlugin)
