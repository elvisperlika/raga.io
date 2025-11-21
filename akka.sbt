import sbt._

val token: String = sys.env.getOrElse("AKKA_TOKEN", "")

ThisBuild / resolvers ++= Seq(
  "akka-secure-mvn" at s"https://repo.akka.io/$token/secure",
  Resolver.url("akka-secure-ivy", url(s"https://repo.akka.io/$token/secure"))(
    Resolver.ivyStylePatterns
  )
)
