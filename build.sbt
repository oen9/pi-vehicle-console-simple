resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "oen",
      scalaVersion := "2.12.7",
      version := "0.1.0-SNAPSHOT",
      scalacOptions ++= Seq(
        "-Xlint",
        "-unchecked",
        "-deprecation",
        "-feature",
        "-Ypartial-unification",
        "-language:higherKinds"
      )
    )),
    name := "pi-vehicle-console-simple",
    libraryDependencies ++= Seq(
      "jline" % "jline" % "2.14.6",
      "org.typelevel" %% "cats-core" % "1.4.0",
      "org.typelevel" %% "cats-effect" % "1.0.0",
      "com.pi4j" % "pi4j-parent" % "1.2-SNAPSHOT",
      "com.pi4j" % "pi4j-core" % "1.2-SNAPSHOT",
      "com.github.pureconfig" %% "pureconfig" % "0.9.2",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  ).enablePlugins(JavaAppPackaging)
