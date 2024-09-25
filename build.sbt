import CiCommands.{ ciBuild, devBuild }

lazy val app =
  project
    .in(file("app"))

commands ++= Seq(ciBuild, devBuild)

scalafmtOnCompile := true
scalafmtConfig := file(".scalafmt.conf")
