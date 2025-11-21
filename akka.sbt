import sbt._

val token: String = sys.env.get("AKKA_TOKEN") match {
  case Some(t) if t.nonEmpty => t
  case _ => sys.error("Environment variable AKKA_TOKEN is not set or is empty")
}

val encodedToken: String = java.net.URLEncoder.encode(token, "UTF-8")

ThisBuild / resolvers ++= Seq(
  "akka-secure-mvn" at s"https://repo.akka.io/$encodedToken/secure",
  Resolver.url("akka-secure-ivy", url(s"https://repo.akka.io/$encodedToken/secure"))(
    Resolver.ivyStylePatterns
  )
)
