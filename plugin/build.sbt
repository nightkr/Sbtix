sbtPlugin := true

name := "sbtix"
organization := "se.nullable.sbtix"
version := "0.2-SNAPSHOT"

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

ScriptedPlugin.scriptedSettings
scriptedLaunchOpts ++= Seq(
  s"-Dplugin.version=${version.value}"
)
scriptedBufferLog := false

publishMavenStyle := false

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

unmanagedResourceDirectories in Compile += baseDirectory.value / "nix-exprs"
