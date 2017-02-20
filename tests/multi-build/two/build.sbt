val org = "sbtix-test-multibuild"

val ver = "0.1.0-SNAPSHOT"

organization := org

name := "mb-two"

version := ver

libraryDependencies += org %% "mb-one" % ver extra ("nix" -> "")

projectID := projectID.value.extra("nix" -> "")
