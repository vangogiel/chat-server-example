import sbt.*

object Dependencies {
  val Scala2 = "2.13.10"

  val Versions = new {
    val Scala3 = "3.3.0"
    val Http4s = "0.23.23"
    val weaver = "0.8.3"
    val cats = "3.2.0"
    val fs2Core = "3.1.4"
    val graphCore = "1.13.2"
    val pureconfig = "0.17.1"
    val sttp = "3.9.2"
  }

  object Compile {
    val grpcNettyShaded = "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    val http4s = "org.http4s" %% "http4s-ember-server" % Versions.Http4s
    val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.Http4s
    val https4sCirce = "org.http4s" %% "http4s-circe" % Versions.Http4s
    val catsEffect = "org.typelevel" %% "cats-effect" % Versions.cats
    val fs2Core = "co.fs2" %% "fs2-core" % Versions.fs2Core
    val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureconfig
    val sttpCore = "com.softwaremill.sttp.client3" %% "core" % Versions.sttp
    val sttpHttpClient = "com.softwaremill.sttp.client3" %% "armeria-backend" % Versions.sttp
    val lyrantheFs2 = "org.lyranthe.fs2-grpc" %% "java-runtime" % "1.0.1"
    val ioGrpc = "io.grpc" % "grpc-core" % "1.40.1"
  }
}
