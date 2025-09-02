ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalafmtOnCompile := true

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
lazy val akkaVersion = "2.10.9"
lazy val root = (project in file("."))
  .settings(
    name := "agar-io",
    assembly / mainClass := Some("it.unibo.test.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion, // For standard log configuration
      "com.typesafe.akka" %% "akka-remote" % akkaVersion, // For akka remote
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion, // akka clustering module
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
    )
  )

enablePlugins(AssemblyPlugin)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case "rootdoc.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
scalacOptions += "-Wunused:imports"
