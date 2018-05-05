libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

resolvers += Resolver.typesafeIvyRepo("releases")

// addSbtPlugin("se.nullable.sbtix" % "sbtix" % "0.2-SNAPSHOT")
