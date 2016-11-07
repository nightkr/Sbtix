sbtPlugin := true

name := "sbtix"
organization := "se.nullable.sbtix"
version := "0.2-SNAPSHOT"

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M14")

resolvers += Resolver.typesafeIvyRepo("releases")

ScriptedPlugin.scriptedSettings
scriptedLaunchOpts ++= Seq(
  s"-Dplugin.version=${version.value}"
)
scriptedBufferLog := false

publishTo := Some(Resolver.file("file", new File( "target/sbtixRepo" ))(Resolver.ivyStylePatterns))

publishMavenStyle := false

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false
