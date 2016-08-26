sbtPlugin := true

name := "sbtix"
organization := "se.nullable.sbtix"
version := "0.1-SNAPSHOT"

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M13")

ScriptedPlugin.scriptedSettings
scriptedLaunchOpts ++= Seq(
  s"-Dplugin.version=${version.value}"
)
scriptedBufferLog := false
