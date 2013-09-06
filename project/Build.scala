import sbt._

object Build extends Build {
  import Keys._

  private val libDeps = Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.2.5",
    "org.apache.httpcomponents" % "httpmime" % "4.2.5"
  )

  lazy val main =
    Project("simplehttp", file(".")).settings(
      organization := "jp.gr.kmc.starlight",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.2",
      libraryDependencies ++= libDeps
    )
}
