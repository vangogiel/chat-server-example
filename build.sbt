import CiCommands.{ ciBuild, devBuild }
import Dependencies.Versions

ThisBuild / scalaVersion := Versions.Scala2

lazy val app =
  project
    .in(file("app"))
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.Compile.grpcNettyShaded,
        Dependencies.Compile.catsEffect,
        Dependencies.Compile.fs2Core,
        Dependencies.Compile.fs2Io,
        Dependencies.Compile.doobieCore,
        Dependencies.Compile.doobieHikari,
        Dependencies.Compile.doobiePostgres,
        Dependencies.Compile.doobieCirce,
        Dependencies.Compile.doobieFlyway,
        Dependencies.Compile.postgresql,
        "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion,
        "com.thesamet.scalapb"  %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
      ),
    )
    .enablePlugins(Fs2Grpc)

commands ++= Seq(ciBuild, devBuild)

scalafmtOnCompile := true
scalafmtConfig := file(".scalafmt.conf")
