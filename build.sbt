import CiCommands.{ ciBuild, devBuild }

lazy val app =
  project
    .in(file("app"))
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.Compile.grpcNettyShaded,
        Dependencies.Compile.catsEffect,
        Dependencies.Compile.fs2Core,
        "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion
      )
    )
    .enablePlugins(Fs2Grpc)

commands ++= Seq(ciBuild, devBuild)

scalafmtOnCompile := true
scalafmtConfig := file(".scalafmt.conf")
