import CiCommands.{ ciBuild, devBuild }

lazy val app =
  project
    .in(file("app"))
    .enablePlugins(Fs2Grpc)

commands ++= Seq(ciBuild, devBuild)

scalafmtOnCompile := true
scalafmtConfig := file(".scalafmt.conf")
