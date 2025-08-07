import sbt.*

object Dependencies {
  val Versions = new {
    val Scala2 = "2.13.16"
    val Http4s = "0.23.23"
    val weaver = "0.8.3"
    val cats = "3.5.4"
    val fs2Core = "3.10.2"
    val graphCore = "1.13.2"
    val pureconfig = "0.17.1"
    val sttp = "3.9.2"
    val doobie = "1.0.0-RC3"
    val postgres = "42.7.5"
  }

  object Compile {
    val grpcNettyShaded = "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    val http4s = "org.http4s" %% "http4s-ember-server" % Versions.Http4s
    val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.Http4s
    val https4sCirce = "org.http4s" %% "http4s-circe" % Versions.Http4s
    val catsEffect = "org.typelevel" %% "cats-effect" % Versions.cats
    val fs2Core = "co.fs2" %% "fs2-core" % Versions.fs2Core
    val fs2Io = "co.fs2" %% "fs2-reactive-streams" % Versions.fs2Core
    val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureconfig
    val sttpCore = "com.softwaremill.sttp.client3" %% "core" % Versions.sttp
    val sttpHttpClient = "com.softwaremill.sttp.client3" %% "armeria-backend" % Versions.sttp
    val lyrantheFs2 = "org.lyranthe.fs2-grpc" %% "java-runtime" % "1.0.1"
    val ioGrpc = "io.grpc" % "grpc-core" % "1.74.0"
    val grpcServices = "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion
    val doobieCore = "org.tpolecat" %% "doobie-core" % Versions.doobie
    val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie
    val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
    val doobieCirce = "org.tpolecat" %% "doobie-postgres-circe" % Versions.doobie
    val postgresql = "org.postgresql" % "postgresql" % Versions.postgres
    val doobieFlyway = "de.lhns" % "doobie-flyway_2.13" % "0.4.0"
  }
}
