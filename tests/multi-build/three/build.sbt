val org = "sbtix-test-multibuild"

val ver = "0.1.0-SNAPSHOT"

organization := org

name := "mb-three"

version := ver

libraryDependencies += org %% "mb-two" % ver extra ("nix" -> "")

enablePlugins(JavaAppPackaging)
